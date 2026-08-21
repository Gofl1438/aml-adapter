package org.example.amladapter.result;

public sealed interface TimerCheckResult{
    record Allowed() implements TimerCheckResult {
    }

    record TooEarly(long retryAfter) implements TimerCheckResult {
    }
}