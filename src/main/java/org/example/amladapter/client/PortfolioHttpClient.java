package org.example.amladapter.client;

import org.example.amladapter.dto.Client;
import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import java.util.Map;


@Component
public class PortfolioHttpClient implements PortfolioClient {
    private final RestTemplate restTemplate;
    private final String portfolioUrl;
    private static final Logger log =
            LoggerFactory.getLogger(PortfolioHttpClient.class);

    public PortfolioHttpClient(
            RestTemplate restTemplate,
            @Value("${portfolio.url:http://localhost:8081/api/v1/clients}") String portfolioUrl
    ) {
        this.restTemplate = restTemplate;
        this.portfolioUrl = portfolioUrl;
    }

    @Override
    public GetClientResult getClient(long id) {
        try {
            String url = portfolioUrl + "/" + id;
            ResponseEntity<Client> response = restTemplate.getForEntity(url, Client.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                return new GetClientResult.Success(response.getBody());
            }
            return new GetClientResult.TechnicalError();

        } catch (HttpClientErrorException.NotFound e) {
            return new GetClientResult.NotFound();
        } catch (RestClientException e) {
            log.error("Ошибка получения клиента из Портфеля: id={}", id, e);
            return new GetClientResult.TechnicalError();
        }
    }

    @Override
    public UpdateAmlResult updateAmlStatus(long id, boolean amlStatus) {
        String url = portfolioUrl + "/" + id + "/aml-status";
        Map<String, Boolean> requestBody = Map.of("amlStatus", amlStatus);
        try {
            restTemplate.patchForObject(url, requestBody, Void.class);
            return new UpdateAmlResult.Success();
        } catch (HttpClientErrorException.NotFound e) {
            return new UpdateAmlResult.NotFound();
        } catch (RestClientException e) {
            return new UpdateAmlResult.TechnicalError();
        }
    }

    @Override
    public UpdateAmlResult markAmlCheckAttempt(long id) {
        String url = portfolioUrl + "/" + id + "/aml-check-at";
        try {
            restTemplate.patchForObject(url, null, Void.class);
            return new UpdateAmlResult.Success();
        } catch (HttpClientErrorException.NotFound e) {
            return new UpdateAmlResult.NotFound();
        } catch (RestClientException e) {
            return new UpdateAmlResult.TechnicalError();
        }
    }
}
