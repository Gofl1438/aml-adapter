package org.example.amladapter.client;

import org.example.amladapter.dto.Client;
import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlStatusResult;
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
            return new GetClientResult.TechnicalError();
        }
    }

    @Override
    public UpdateAmlStatusResult updateAmlStatus(long id, boolean amlStatus) {
        try {
            String url = portfolioUrl + "/" + id + "/aml-status";
            Map<String, Boolean> requestBody = Map.of("amlStatus", amlStatus);
            restTemplate.patchForObject(url, requestBody, Void.class);
            return new UpdateAmlStatusResult.Success();
        } catch (HttpClientErrorException.NotFound e) {
            return new UpdateAmlStatusResult.NotFound();
        } catch (RestClientException e) {
            return new UpdateAmlStatusResult.TechnicalError();
        }
    }
}
