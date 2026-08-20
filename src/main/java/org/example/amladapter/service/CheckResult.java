package org.example.amladapter.service;

public sealed interface CheckResult
        permits CheckResult.Success,
        CheckResult.ClientNotFound,
        CheckResult.TooEarly,
        CheckResult.ProcessingError,
        CheckResult.ServiceUnavailable {

    record Success(boolean amlStatus) implements CheckResult {
    }

    record ClientNotFound() implements CheckResult {
    }

    record TooEarly(long retryAfter) implements CheckResult {
    }

    record ProcessingError() implements CheckResult {
    }

    record ServiceUnavailable() implements CheckResult {
    }
}
