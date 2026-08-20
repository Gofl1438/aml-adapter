package org.example.amladapter.service;

import org.example.amladapter.client.CreditBureauClient;
import org.example.amladapter.client.PortfolioClient;
import org.example.amladapter.result.TimerCheckResult;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class ClientCheckService {
    private final PortfolioClient portfolioClient;
    private final CreditBureauClient creditBureauClient;

    public ClientCheckService(PortfolioClient portfolioClient, CreditBureauClient creditBureauClient){
        this.portfolioClient = portfolioClient;
        this.creditBureauClient = creditBureauClient;
    }

    public CheckResult checkClient(Long id){

        // TO DO

        return null;
    }

    private TimerCheckResult checkTimer(Instant amlCheckedAt){

        // TO DO

        return null;
    }
}
