package org.example.amladapter.client;

import org.example.amladapter.dto.CreditBureauRequest;
import org.example.amladapter.result.AmlCheckResult;
import org.springframework.stereotype.Component;

@Component
public class CreditBureauHttpClient implements CreditBureauClient {
    @Override
    public AmlCheckResult checkStatus(
            CreditBureauRequest request
    ) {

        // SOAP-запрос в Бюро

        return null;
    }
}