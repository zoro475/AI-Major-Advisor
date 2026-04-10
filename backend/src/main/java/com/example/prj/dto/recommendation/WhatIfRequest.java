package com.example.prj.dto.recommendation;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record WhatIfRequest(
        @NotNull Long submissionId,
        List<String> addSkills,
        List<String> removeSkills,
        List<String> newInterests,
        String newPersonality
) {}
