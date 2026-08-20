package org.example.amladapter.result;

public sealed interface UpdateAmlStatusResult
        permits UpdateAmlStatusResult.Success,
        UpdateAmlStatusResult.NotFound,
        UpdateAmlStatusResult.ServerError {

    record Success() implements UpdateAmlStatusResult {
    }

    record NotFound() implements UpdateAmlStatusResult {
    }

    record ServerError() implements UpdateAmlStatusResult {
    }
}
