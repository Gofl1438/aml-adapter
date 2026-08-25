package org.example.amladapter.client;

import org.example.amladapter.dto.CreditBureauRequest;
import org.example.amladapter.result.AmlCheckResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class CreditBureauHttpClient implements CreditBureauClient {

    private final RestTemplate restTemplate;
    private final String creditBureauUrl;
    private static final Logger log =
            LoggerFactory.getLogger(PortfolioHttpClient.class);

    public CreditBureauHttpClient(
            RestTemplate restTemplate,
            @Value("${credit-bureau.url:http://localhost:8082/check}") String creditBureauUrl
    ) {
        this.restTemplate = restTemplate;
        this.creditBureauUrl = creditBureauUrl;
    }

    private static final int MAX_RETRIES = 5;

    @Override
    public AmlCheckResult checkStatus(CreditBureauRequest request) {
        AmlCheckResult lastResult = new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            lastResult = doRequest(request);

            if (lastResult instanceof AmlCheckResult.Success) {
                return lastResult;
            }
            if (lastResult instanceof AmlCheckResult.TechnicalError technicalError
                    && technicalError.code() == AmlCheckResult.TechnicalError.ErrorCode.VALIDATION_ERROR) {
                return lastResult;
            }
            // SERVICE_ERROR — пробуем ещё раз
        }

        return lastResult;
    }

    private AmlCheckResult doRequest(CreditBureauRequest request) {
        try {
            String xmlBody = buildXmlRequestBody(request);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.TEXT_XML);

            HttpEntity<String> entity = new HttpEntity<>(xmlBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(creditBureauUrl, entity, String.class);

            if (response.getBody() != null) {
                return parseXmlResponse(response.getBody());
            }

            return new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR);

        } catch (RestClientException e) {
            return new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR);
        }
    }

    private String buildXmlRequestBody(CreditBureauRequest request) {
        return "<checkClient>" +
                "<fio>" + (request.fio() != null ? request.fio() : "") + "</fio>" +
                "<inn>" + (request.inn() != null ? request.inn() : "") + "</inn>" +
                "<snils>" + (request.snils() != null ? request.snils() : "") + "</snils>" +
                "</checkClient>";
    }

    private AmlCheckResult parseXmlResponse(String xml) {
        if (xml.contains("<faultcode>soap:Client</faultcode>")) {
            return new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.VALIDATION_ERROR);
        }
        if (xml.contains("<faultcode>soap:Server</faultcode>")) {
            return new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR);
        }
        if (xml.contains("<amlStatus>true</amlStatus>")) {
            return new AmlCheckResult.Success(true);
        }
        if (xml.contains("<amlStatus>false</amlStatus>")) {
            return new AmlCheckResult.Success(false);
        }

        return new AmlCheckResult.TechnicalError(AmlCheckResult.TechnicalError.ErrorCode.SERVICE_ERROR);
    }
}