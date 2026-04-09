package com.example.prj.dto.survey;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AnswerRequest(
        @NotNull(message = "ID câu hỏi không được để trống")
        Long questionId,

        @NotBlank(message = "Câu trả lời không được để trống")
        String answerValue
) {}
