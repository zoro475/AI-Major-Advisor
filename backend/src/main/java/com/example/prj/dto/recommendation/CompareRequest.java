package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;

public record CompareRequest(
        @NotNull(message = "submissionId không được để trống")
        Long submissionId,

        @NotNull(message = "majorId1 không được để trống")
        Long majorId1,

        @NotNull(message = "majorId2 không được để trống")
        Long majorId2
) {}
