package com.example.prj.dto.survey;

import com.example.prj.entity.QuestionType;

import java.time.LocalDateTime;

public record AnswerResponse(
        Long id,
        Long questionId,
        String questionText,            // Snapshot để hiển thị lịch sử
        QuestionType questionType,
        String answerValue,
        LocalDateTime createdAt
) {}
