package org.example.amladapter.result;

public sealed interface UpdateAmlStatusResult {
    record Success() implements UpdateAmlStatusResult {
    }

    record NotFound() implements UpdateAmlStatusResult {
    }

    record TechnicalError() implements UpdateAmlStatusResult {
    }
}
