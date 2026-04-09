package com.example.prj.dto.survey;

import com.example.prj.entity.QuestionType;

import java.time.LocalDateTime;
import java.util.List;

public record SurveyQuestionResponse(
        Long id,
        QuestionType questionType,
        String questionText,
        List<String> options,           // Đã parse từ JSON
        Integer maxRating,
        Integer displayOrder,
        Boolean isRequired,
        Boolean isActive,
        LocalDateTime createdAt
) {}
