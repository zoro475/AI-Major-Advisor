package com.example.prj.dto.survey;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SurveySubmissionRequest(
        @NotBlank(message = "Tên học sinh không được để trống")
        String studentName,

        String studentEmail,

        String freeTextDescription,     // Mô tả tự do của học sinh

        @NotEmpty(message = "Phải trả lời ít nhất 1 câu hỏi")
        @Valid
        List<AnswerRequest> answers
) {}
