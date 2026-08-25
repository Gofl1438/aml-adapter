package org.example.amladapter.client;

import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlResult;

public interface PortfolioClient {
    GetClientResult getClient(long id);
    UpdateAmlResult updateAmlStatus(long id, boolean amlStatus);
    UpdateAmlResult markAmlCheckAttempt(long id);
}
