package org.example.amladapter.client;

import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlStatusResult;
import org.springframework.stereotype.Component;

@Component
public class PortfolioHttpClient implements PortfolioClient {

    @Override
    public GetClientResult getClient(Long id) {

        // REST-запрос в Portfolio

        return null;
    }

    @Override
    public UpdateAmlStatusResult updateAmlStatus(
            Long id,
            boolean amlStatus
    ) {

        // REST-запрос в Portfolio

        return null;
    }
}
