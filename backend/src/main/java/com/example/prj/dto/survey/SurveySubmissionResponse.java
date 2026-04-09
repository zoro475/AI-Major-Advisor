package com.example.prj.dto.survey;

import java.time.LocalDateTime;
import java.util.List;

public record SurveySubmissionResponse(
        Long id,
        String studentName,
        String studentEmail,
        String freeTextDescription,
        LocalDateTime submittedAt,
        List<AnswerResponse> answers
) {}
