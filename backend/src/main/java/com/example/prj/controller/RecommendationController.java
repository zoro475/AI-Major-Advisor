package com.example.prj.controller;

import com.example.prj.dto.recommendation.*;
import com.example.prj.service.RecommendationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    /**
     * Phân tích AI và đề xuất ngành học phù hợp.
     */
    @PostMapping("/analyze")
    public ResponseEntity<RecommendationResponse> analyze(
            @Valid @RequestBody RecommendationRequest request) {
        return ResponseEntity.ok(recommendationService.analyze(request.submissionId()));
    }

    /**
     * Lấy kết quả recommendation đã lưu.
     */
    @GetMapping("/{submissionId}")
    public ResponseEntity<RecommendationResponse> getBySubmissionId(
            @PathVariable Long submissionId) {
        return ResponseEntity.ok(recommendationService.getBySubmissionId(submissionId));
    }

    /**
     * So sánh 2 ngành học dựa trên profile học sinh.
     */
    @PostMapping("/compare")
    public ResponseEntity<CompareResponse> compare(
            @Valid @RequestBody CompareRequest request) {
        return ResponseEntity.ok(recommendationService.compare(
                request.submissionId(), request.majorId1(), request.majorId2()));
    }

    /**
     * Tạo lộ trình nghề nghiệp cho 1 ngành.
     */
    @PostMapping("/roadmap")
    public ResponseEntity<RoadmapResponse> getRoadmap(
            @Valid @RequestBody RoadmapRequest request) {
        return ResponseEntity.ok(recommendationService.getRoadmap(
                request.submissionId(), request.majorId()));
    }

    /**
     * Future-Proof Scorecard cho 1 ngành.
     */
    @PostMapping("/scorecard")
    public ResponseEntity<ScorecardResponse> getScorecard(
            @Valid @RequestBody ScorecardRequest request) {
        return ResponseEntity.ok(recommendationService.getScorecard(
                request.submissionId(), request.majorId()));
    }
}
