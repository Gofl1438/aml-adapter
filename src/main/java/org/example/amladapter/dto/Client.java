package org.example.amladapter.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.Instant;

public record Client(
        long id,
        String lastName,
        String firstName,
        String patronymic,
        String inn,
        String snils,
        @JsonAlias("lastAmlCheck")
        Instant amlCheckedAt
) {
}
