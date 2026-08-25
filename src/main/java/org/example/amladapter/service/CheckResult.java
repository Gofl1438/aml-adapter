package org.example.amladapter.service;

public sealed interface CheckResult {
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

    record RetryRequired(long retryAfter) implements CheckResult {}
}
