package org.example.amladapter.service;

import org.example.amladapter.client.CreditBureauClient;
import org.example.amladapter.client.PortfolioClient;
import org.example.amladapter.dto.Client;
import org.example.amladapter.dto.CreditBureauRequest;
import org.example.amladapter.result.AmlCheckResult;
import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.TimerCheckResult;
import org.example.amladapter.result.UpdateAmlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class ClientCheckService {
    private static final Logger log = LoggerFactory.getLogger(ClientCheckService.class);
    private final PortfolioClient portfolioClient;
    private final CreditBureauClient creditBureauClient;

    public ClientCheckService(PortfolioClient portfolioClient, CreditBureauClient creditBureauClient){
        this.portfolioClient = portfolioClient;
        this.creditBureauClient = creditBureauClient;
    }

    public CheckResult checkClient(long id){
        log.info("Начало проверки клиента: id={}", id);
        // Этап получения клиента
        log.info("Получение клиента: id={}", id);

        GetClientResult getClientResult = portfolioClient.getClient(id);

        if (getClientResult instanceof GetClientResult.NotFound) {
            log.error("Клиент не найден: id={}", id);
            return new CheckResult.ClientNotFound();
        }
        if (getClientResult instanceof GetClientResult.TechnicalError) {
            log.error("Ошибка при получении клиента: id={}", id);
            return new CheckResult.ServiceUnavailable();
        }

        GetClientResult.Success success = (GetClientResult.Success) getClientResult;

        Client client = success.client();
        log.info("Клиент успешно получен: id={}", client.id());

        // Этап разрешения на проверку

        TimerCheckResult timerCheckResult = checkTimer(client.amlCheckedAt());

        if(timerCheckResult instanceof TimerCheckResult.TooEarly tooEarly) {
            log.info(
                    "AML-проверка пока недоступна: id={}, retryAfter={} сек.",
                    id,
                    tooEarly.retryAfter()
            );
            return new CheckResult.TooEarly(tooEarly.retryAfter());
        }

        log.info("AML-проверка разрешена: id={}", id);

        // Этап проверки AML-статуса

        log.info("Запрос AML-статуса: id={}", id);

        String fullFio = buildFio(client.lastName(), client.firstName(), client.patronymic());

        CreditBureauRequest request = new CreditBureauRequest(
                fullFio,
                client.inn(),
                client.snils()
        );

        AmlCheckResult amlCheckResult = creditBureauClient.checkStatus(request);

        if (amlCheckResult instanceof AmlCheckResult.TechnicalError technicalError){
            log.error(
                    "Ошибка AML-сервиса: id={}, code={}",
                    id,
                    technicalError.code()
            );
            if(technicalError.code() == AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR){
                CheckResult amlTimerResult = markAmlCheckAttempt(id);

                if (amlTimerResult != null) {
                    return amlTimerResult;
                }

                return new CheckResult.RetryRequired(300);
            }
            if(technicalError.code() == AmlCheckResult.TechnicalError.ErrorCode.VALIDATION_ERROR){
                return new CheckResult.ProcessingError();
            }
        }

        CheckResult amlTimerResult = markAmlCheckAttempt(id);

        if (amlTimerResult != null) {
            return amlTimerResult ;
        }

        AmlCheckResult.Success amlCheckSuccess = (AmlCheckResult.Success) amlCheckResult;

        log.info(
                "AML-статус получен: id={}, status={}",
                id,
                amlCheckSuccess.amlStatus()
        );

        // Этап обновления AML-статуса

        log.info("Обновление AML-статуса клиента: id={}", id);

        UpdateAmlResult updateAmlStatusResult = portfolioClient.updateAmlStatus(client.id(), amlCheckSuccess.amlStatus());

        if (updateAmlStatusResult instanceof UpdateAmlResult.NotFound){
            log.error("Клиент не найден при обновлении AML-статуса: id={}", id);
            return new CheckResult.RetryRequired(300);
        }
        if (updateAmlStatusResult instanceof UpdateAmlResult.TechnicalError){
            log.error("Ошибка обновления AML-статуса: id={}", id);
            return new CheckResult.RetryRequired(300);
        }

        log.info(
                "AML-статус успешно обновлён: id={}",
                id
        );

        log.info("Проверка клиента завершена успешно: id={}", id);

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

    private String buildFio(String lastName, String firstName, String patronymic) {
        if (patronymic == null || patronymic.isBlank()) {
            return (lastName + " " + firstName).trim();
        }
        return (lastName + " " + firstName + " " + patronymic).trim();
    }

    private CheckResult markAmlCheckAttempt(long id) {
        UpdateAmlResult result =
                portfolioClient.markAmlCheckAttempt(id);

        if (result instanceof UpdateAmlResult.NotFound) {
            log.error("Клиент не найден при записи времени: id={}", id);
            return new CheckResult.ClientNotFound();
        }

        if (result instanceof UpdateAmlResult.TechnicalError) {
            log.error("Ошибка записи времени: id={}", id);
            return new CheckResult.ServiceUnavailable();
        }

        log.info(
                "Время проверки успешно обновлено: id={}",
                id
        );
        return null;
    }


}
