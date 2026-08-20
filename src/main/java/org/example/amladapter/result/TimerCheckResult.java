package org.example.amladapter.result;

public sealed interface TimerCheckResult
        permits TimerCheckResult.Allowed,
        TimerCheckResult.TooEarly {

    record Allowed() implements TimerCheckResult {
    }

    record TooEarly(long retryAfter) implements TimerCheckResult {
    }
}