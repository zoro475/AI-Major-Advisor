package com.example.prj.entity;

public enum QuestionType {
    SINGLE_CHOICE,      // Radio buttons - chọn 1
    MULTIPLE_CHOICE,    // Checkboxes - chọn nhiều
    RATING,             // Star rating (1-N)
    RATING_MATRIX,      // Star rating cho TỪNG option (Toán: 4★, Lý: 3★, ...)
    TEXTAREA            // Free text - tự luận
}

