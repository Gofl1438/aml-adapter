package org.example.amladapter.dto;

import java.time.Instant;

public record Client(
        Long id,
        String fio,
        String inn,
        String snils,
        Instant amlCheckedAt
) {
}
