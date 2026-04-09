package com.example.prj.dto.recommendation;

import java.util.List;

public record ScorecardResponse(
        Long majorId,
        String majorName,
        Integer overallScore,                    // Điểm tổng 0-100
        String overallVerdict,                   // "Rất tốt" / "Tốt" / "Khá"
        List<ScoreMetric> metrics,
        String aiInsight                         // Nhận xét tổng quan từ AI
) {
    public record ScoreMetric(
            String name,                          // Tên chỉ số
            String icon,                          // Emoji icon
            Integer score,                        // 0-100
            String label,                         // "Xuất sắc" / "Tốt" / "Trung bình"
            String explanation                    // Giải thích ngắn
    ) {}
}
