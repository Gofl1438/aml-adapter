package org.example.amladapter.dto;

import java.time.Instant;

public record Client(
        long id,
        String lastName,
        String firstName,
        String middleName,
        String inn,
        String snils,
        Instant amlCheckedAt
) {
}
