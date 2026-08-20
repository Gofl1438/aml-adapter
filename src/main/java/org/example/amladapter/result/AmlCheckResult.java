package org.example.amladapter.result;

public sealed interface AmlCheckResult
        permits AmlCheckResult.Success,
        AmlCheckResult.ValidationError,
        AmlCheckResult.ServerError {

    record Success(boolean amlStatus) implements AmlCheckResult {
    }

    record ValidationError() implements AmlCheckResult {
    }

    record ServerError() implements AmlCheckResult {
    }
}