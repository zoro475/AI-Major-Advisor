package com.example.prj.service;

import com.example.prj.dto.survey.SurveyQuestionRequest;
import com.example.prj.dto.survey.SurveyQuestionResponse;
import com.example.prj.entity.QuestionType;
import com.example.prj.entity.SurveyQuestion;
import com.example.prj.repository.SurveyQuestionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SurveyQuestionService {

    private final SurveyQuestionRepository questionRepo;
    private final ObjectMapper objectMapper;

    /**
     * Lấy tất cả câu hỏi (Admin)
     */
    public List<SurveyQuestionResponse> getAll() {
        return questionRepo.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy câu hỏi theo ID
     */
    public SurveyQuestionResponse getById(Long id) {
        SurveyQuestion question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));
        return toResponse(question);
    }

    /**
     * Lấy câu hỏi active (cho học sinh làm khảo sát)
     */
    public List<SurveyQuestionResponse> getActiveQuestions() {
        return questionRepo.findByIsActiveTrueOrderByDisplayOrderAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Tạo câu hỏi mới
     */
    @Transactional
    public SurveyQuestionResponse create(SurveyQuestionRequest request) {
        validateRequest(request);

        SurveyQuestion question = SurveyQuestion.builder()
                .questionType(request.questionType())
                .questionText(request.questionText())
                .options(serializeOptions(request.options()))
                .maxRating(request.questionType() == QuestionType.RATING || request.questionType() == QuestionType.RATING_MATRIX
                        ? (request.maxRating() != null ? request.maxRating() : 5)
                        : null)
                .displayOrder(request.displayOrder() != null ? request.displayOrder() : 0)
                .isRequired(request.isRequired() != null ? request.isRequired() : true)
                .isActive(true)
                .build();

        question = questionRepo.save(question);
        log.info("Tạo câu hỏi mới: ID={}, Type={}", question.getId(), question.getQuestionType());
        return toResponse(question);
    }

    /**
     * Cập nhật câu hỏi
     */
    @Transactional
    public SurveyQuestionResponse update(Long id, SurveyQuestionRequest request) {
        validateRequest(request);

        SurveyQuestion question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));

        question.setQuestionType(request.questionType());
        question.setQuestionText(request.questionText());
        question.setOptions(serializeOptions(request.options()));
        question.setMaxRating(request.questionType() == QuestionType.RATING || request.questionType() == QuestionType.RATING_MATRIX
                ? (request.maxRating() != null ? request.maxRating() : 5)
                : null);
        question.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : question.getDisplayOrder());
        question.setIsRequired(request.isRequired() != null ? request.isRequired() : question.getIsRequired());

        question = questionRepo.save(question);
        log.info("Cập nhật câu hỏi: ID={}", question.getId());
        return toResponse(question);
    }

    /**
     * Xóa câu hỏi (soft delete - chuyển isActive = false)
     */
    @Transactional
    public void delete(Long id) {
        SurveyQuestion question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));
        question.setIsActive(false);
        questionRepo.save(question);
        log.info("Soft delete câu hỏi: ID={}", id);
    }

    /**
     * Bật/tắt trạng thái active
     */
    @Transactional
    public SurveyQuestionResponse toggleActive(Long id) {
        SurveyQuestion question = questionRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi với ID: " + id));
        question.setIsActive(!question.getIsActive());
        question = questionRepo.save(question);
        log.info("Toggle active câu hỏi: ID={}, isActive={}", id, question.getIsActive());
        return toResponse(question);
    }

    // ==================== Helper Methods ====================

    private void validateRequest(SurveyQuestionRequest request) {
        QuestionType type = request.questionType();

        if ((type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTIPLE_CHOICE || type == QuestionType.RATING_MATRIX)
                && (request.options() == null || request.options().isEmpty())) {
            throw new IllegalArgumentException(
                    "Câu hỏi loại " + type + " phải có ít nhất 1 lựa chọn (options)");
        }

        if ((type == QuestionType.RATING || type == QuestionType.RATING_MATRIX) && request.maxRating() != null
                && (request.maxRating() < 1 || request.maxRating() > 10)) {
            throw new IllegalArgumentException("maxRating phải nằm trong khoảng 1-10");
        }
    }

    private String serializeOptions(List<String> options) {
        if (options == null || options.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(options);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Lỗi serialize options", e);
        }
    }

    private List<String> deserializeOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(optionsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            log.warn("Lỗi deserialize options: {}", optionsJson, e);
            return Collections.emptyList();
        }
    }

    private SurveyQuestionResponse toResponse(SurveyQuestion entity) {
        return new SurveyQuestionResponse(
                entity.getId(),
                entity.getQuestionType(),
                entity.getQuestionText(),
                deserializeOptions(entity.getOptions()),
                entity.getMaxRating(),
                entity.getDisplayOrder(),
                entity.getIsRequired(),
                entity.getIsActive(),
                entity.getCreatedAt()
        );
    }
}
