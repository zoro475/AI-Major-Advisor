package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TimeMachineRequest(
        @NotNull Long submissionId,
        @NotNull Long majorId,
        List<String> customSkills,
        List<String> customInterests
) {}
