package com.example.prj.dto.recommendation;

import java.util.List;

public record WhatIfResponse(
        String studentName,
        String originalProfile,
        String whatIfProfile,
        List<MajorChange> changes,
        String aiAnalysis
) {
    public record MajorChange(
            Long majorId,
            String majorName,
            int originalScore,
            int newScore,
            int scoreDelta,
            String changeReason
    ) {}
}
