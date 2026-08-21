package org.example.amladapter.result;

import org.example.amladapter.dto.Client;

public sealed interface GetClientResult {
    record Success(Client client) implements GetClientResult {
    }

    record NotFound() implements GetClientResult {
    }

    record TechnicalError() implements GetClientResult {
    }
}