package org.example.amladapter.client;

import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlStatusResult;

public interface PortfolioClient {
    GetClientResult getClient(long id);
    UpdateAmlStatusResult updateAmlStatus(long id, boolean amlStatus);
}
