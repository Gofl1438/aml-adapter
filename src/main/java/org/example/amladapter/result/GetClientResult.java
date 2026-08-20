package org.example.amladapter.result;

import org.example.amladapter.dto.Client;

public sealed interface GetClientResult
        permits GetClientResult.Success,
        GetClientResult.NotFound,
        GetClientResult.ServerError {

    record Success(Client client) implements GetClientResult {
    }

    record NotFound() implements GetClientResult {
    }

    record ServerError() implements GetClientResult {
    }
}