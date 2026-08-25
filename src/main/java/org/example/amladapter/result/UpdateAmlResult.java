package org.example.amladapter.result;

public sealed interface UpdateAmlResult {
    record Success() implements UpdateAmlResult {
    }

    record NotFound() implements UpdateAmlResult {
    }

    record TechnicalError() implements UpdateAmlResult {
    }
}
