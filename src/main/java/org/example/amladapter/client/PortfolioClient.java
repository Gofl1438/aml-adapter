package org.example.amladapter.client;

import org.example.amladapter.result.GetClientResult;
import org.example.amladapter.result.UpdateAmlStatusResult;

public interface PortfolioClient {
    GetClientResult getClient(Long id);
    UpdateAmlStatusResult updateAmlStatus(Long id, boolean amlStatus);
}
