package com.example.prj.controller;

import com.example.prj.dto.survey.SurveyQuestionRequest;
import com.example.prj.dto.survey.SurveyQuestionResponse;
import com.example.prj.service.SurveyQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/survey-questions")
@RequiredArgsConstructor
public class AdminSurveyQuestionController {

    private final SurveyQuestionService questionService;

    /**
     * Lấy tất cả câu hỏi
     */
    @GetMapping
    public ResponseEntity<List<SurveyQuestionResponse>> getAll() {
        return ResponseEntity.ok(questionService.getAll());
    }

    /**
     * Lấy câu hỏi theo ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<SurveyQuestionResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getById(id));
    }

    /**
     * Tạo câu hỏi mới
     */
    @PostMapping
    public ResponseEntity<SurveyQuestionResponse> create(
            @Valid @RequestBody SurveyQuestionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(questionService.create(request));
    }

    /**
     * Cập nhật câu hỏi
     */
    @PutMapping("/{id}")
    public ResponseEntity<SurveyQuestionResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SurveyQuestionRequest request) {
        return ResponseEntity.ok(questionService.update(id, request));
    }

    /**
     * Xóa câu hỏi (soft delete)
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        questionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Bật/tắt trạng thái active
     */
    @PatchMapping("/{id}/toggle")
    public ResponseEntity<SurveyQuestionResponse> toggleActive(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.toggleActive(id));
    }
}
