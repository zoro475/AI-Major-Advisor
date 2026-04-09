package com.example.prj.dto.survey;

import com.example.prj.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record SurveyQuestionRequest(
        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        String questionText,

        @NotNull(message = "Loại câu hỏi không được để trống")
        QuestionType questionType,

        List<String> options,           // Bắt buộc cho SINGLE_CHOICE, MULTIPLE_CHOICE

        Integer maxRating,              // Bắt buộc cho RATING (default 5)

        Integer displayOrder,

        Boolean isRequired              // default true
) {}
