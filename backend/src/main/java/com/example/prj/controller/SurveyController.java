package com.example.prj.controller;

import com.example.prj.dto.survey.SurveyQuestionResponse;
import com.example.prj.dto.survey.SurveySubmissionRequest;
import com.example.prj.dto.survey.SurveySubmissionResponse;
import com.example.prj.service.SurveyQuestionService;
import com.example.prj.service.SurveyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/survey")
@RequiredArgsConstructor
public class SurveyController {

    private final SurveyService surveyService;
    private final SurveyQuestionService questionService;

    /**
     * Lấy danh sách câu hỏi active (cho học sinh làm khảo sát)
     */
    @GetMapping("/questions")
    public ResponseEntity<List<SurveyQuestionResponse>> getActiveQuestions() {
        return ResponseEntity.ok(questionService.getActiveQuestions());
    }

    /**
     * Học sinh submit bài khảo sát
     */
    @PostMapping("/submit")
    public ResponseEntity<SurveySubmissionResponse> submit(
            @Valid @RequestBody SurveySubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(surveyService.submit(request));
    }

    /**
     * Lấy lịch sử khảo sát theo email
     */
    @GetMapping("/history")
    public ResponseEntity<List<SurveySubmissionResponse>> getHistory(
            @RequestParam String email) {
        return ResponseEntity.ok(surveyService.getHistoryByEmail(email));
    }

    /**
     * Lấy chi tiết 1 bài khảo sát
     */
    @GetMapping("/history/{id}")
    public ResponseEntity<SurveySubmissionResponse> getSubmissionDetail(
            @PathVariable Long id) {
        return ResponseEntity.ok(surveyService.getSubmissionById(id));
    }
}
