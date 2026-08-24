package org.example.amladapter.service;

import org.example.amladapter.client.CreditBureauClient;
import org.example.amladapter.client.PortfolioClient;
import org.example.amladapter.dto.Client;
import org.example.amladapter.dto.CreditBureauRequest;
import org.example.amladapter.result.AmlCheckResult;
import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.TimerCheckResult;
import org.example.amladapter.result.UpdateAmlStatusResult;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ClientCheckService {
    private final PortfolioClient portfolioClient;
    private final CreditBureauClient creditBureauClient;

    public ClientCheckService(PortfolioClient portfolioClient, CreditBureauClient creditBureauClient){
        this.portfolioClient = portfolioClient;
        this.creditBureauClient = creditBureauClient;
    }

    public CheckResult checkClient(long id){
        // Этап получения клиента

        GetClientResult getClientResult = portfolioClient.getClient(id);

        if (getClientResult instanceof GetClientResult.NotFound) {
            return new CheckResult.ClientNotFound();
        }
        if (getClientResult instanceof GetClientResult.TechnicalError) {
            return new CheckResult.ServiceUnavailable();
        }

        GetClientResult.Success success = (GetClientResult.Success) getClientResult;

        Client client = success.client();

        // Этап разрешения на проверку

        TimerCheckResult timerCheckResult = checkTimer(client.amlCheckedAt());

        if(timerCheckResult instanceof TimerCheckResult.TooEarly tooEarly) {
            return new CheckResult.TooEarly(tooEarly.retryAfter());
        }

        // Этап проверки AML-статуса

        String fullFio = buildFio(client.lastName(), client.firstName(), client.middleName());

        CreditBureauRequest request = new CreditBureauRequest(
                fullFio,
                client.inn(),
                client.snils()
        );

        AmlCheckResult amlCheckResult = creditBureauClient.checkStatus(request);

        if (amlCheckResult instanceof AmlCheckResult.TechnicalError technicalError){
            if(technicalError.code() == AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR){
                return new CheckResult.ServiceUnavailable();
            }
            if(technicalError.code() == AmlCheckResult.TechnicalError.ErrorCode.VALIDATION_ERROR){
                return new CheckResult.ProcessingError();
            }
        }

        if (amlCheckResult instanceof AmlCheckResult.RetryExhausted) {
            return new CheckResult.ResultUndefined();
        }

        AmlCheckResult.Success amlCheckSuccess = (AmlCheckResult.Success) amlCheckResult;

        // Этап обновления AML-статуса

        UpdateAmlStatusResult updateAmlStatusResult = portfolioClient.updateAmlStatus(client.id(), amlCheckSuccess.amlStatus());

        if (updateAmlStatusResult instanceof UpdateAmlStatusResult.NotFound){
            return new CheckResult.ClientNotFound();
        }
        if (updateAmlStatusResult instanceof UpdateAmlStatusResult.TechnicalError){
            return new CheckResult.ServiceUnavailable();
        }

        return new CheckResult.Success(amlCheckSuccess.amlStatus());
    }

    private TimerCheckResult checkTimer(Instant amlCheckedAt){
        if (amlCheckedAt == null) {
            return new TimerCheckResult.Allowed();
        }

        Instant nextCheckAt = amlCheckedAt.plus(5, ChronoUnit.MINUTES);

        Instant now = Instant.now();

        if (now.isBefore(nextCheckAt)) {
            long retryAfter = Duration.between(now, nextCheckAt).getSeconds();

            return new TimerCheckResult.TooEarly(retryAfter);
        }

        return new TimerCheckResult.Allowed();
    }

    private String buildFio(String lastName, String firstName, String middleName) {
        if (middleName == null || middleName.isBlank()) {
            return (lastName + " " + firstName).trim();
        }
        return (lastName + " " + firstName + " " + middleName).trim();
    }
}
