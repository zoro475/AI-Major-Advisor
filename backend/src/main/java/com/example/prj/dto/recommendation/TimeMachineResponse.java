package com.example.prj.dto.recommendation;

import java.util.List;

public record TimeMachineResponse(
        String majorName,
        String studentProfile,
        List<FutureSnapshot> snapshots,
        String overallMessage
) {
    public record FutureSnapshot(
            int yearsFromNow,
            String title,
            String emoji,
            DayInLife dayInLife,
            String salaryRange,
            List<String> achievements,
            List<Challenge> challenges,
            List<String> opportunities,
            WorstCase worstCase
    ) {}

    public record DayInLife(
            String morning,
            String afternoon,
            String evening,
            String highlight
    ) {}

    public record Challenge(
            String name,
            String description,
            String howToOvercome
    ) {}

    public record WorstCase(
            String scenario,
            String consequences,
            String preventionTip
    ) {}
}
