package org.example.amladapter.client;

import org.example.amladapter.result.AmlCheckResult;
import org.example.amladapter.dto.CreditBureauRequest;

public interface CreditBureauClient {
    AmlCheckResult checkStatus(CreditBureauRequest request);
}
