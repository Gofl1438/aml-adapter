package org.example.amladapter.result;

public sealed interface AmlCheckResult {
    record Success(boolean amlStatus) implements AmlCheckResult {
    }

    record TechnicalError(ErrorCode code) implements AmlCheckResult {

        public enum ErrorCode {
            VALIDATION_ERROR,
            SERVICE_ERROR
        }
    }

    record RetryExhausted() implements AmlCheckResult {
    }
}