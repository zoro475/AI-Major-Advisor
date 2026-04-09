package com.example.prj.service;

import com.example.prj.dto.survey.*;
import com.example.prj.entity.SurveyAnswer;
import com.example.prj.entity.SurveyQuestion;
import com.example.prj.entity.SurveySubmission;
import com.example.prj.repository.SurveyQuestionRepository;
import com.example.prj.repository.SurveySubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class SurveyService {

    private final SurveySubmissionRepository submissionRepo;
    private final SurveyQuestionRepository questionRepo;

    /**
     * Học sinh submit bài khảo sát
     */
    @Transactional
    public SurveySubmissionResponse submit(SurveySubmissionRequest request) {
        // 1. Tạo SurveySubmission
        SurveySubmission submission = SurveySubmission.builder()
                .studentName(request.studentName())
                .studentEmail(request.studentEmail())
                .freeTextDescription(request.freeTextDescription())
                .build();

        // 2. Xử lý từng câu trả lời
        for (AnswerRequest answerReq : request.answers()) {
            SurveyQuestion question = questionRepo.findById(answerReq.questionId())
                    .orElseThrow(() -> new RuntimeException(
                            "Không tìm thấy câu hỏi với ID: " + answerReq.questionId()));

            if (!question.getIsActive()) {
                throw new RuntimeException("Câu hỏi ID " + answerReq.questionId() + " đã bị vô hiệu hóa");
            }

            SurveyAnswer answer = SurveyAnswer.builder()
                    .submission(submission)
                    .question(question)
                    .answerValue(answerReq.answerValue())
                    .build();

            submission.getAnswers().add(answer);
        }

        // 3. Save (cascade save answers)
        submission = submissionRepo.save(submission);
        log.info("Học sinh '{}' submit khảo sát thành công (ID={})", request.studentName(), submission.getId());

        return toResponse(submission);
    }

    /**
     * Lấy lịch sử khảo sát theo email
     */
    public List<SurveySubmissionResponse> getHistoryByEmail(String email) {
        return submissionRepo.findByEmailWithAnswers(email).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Lấy chi tiết 1 bài khảo sát
     */
    public SurveySubmissionResponse getSubmissionById(Long id) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát với ID: " + id));
        return toResponse(submission);
    }

    // ==================== Helper Methods ====================

    private SurveySubmissionResponse toResponse(SurveySubmission entity) {
        List<AnswerResponse> answers = entity.getAnswers().stream()
                .map(a -> new AnswerResponse(
                        a.getId(),
                        a.getQuestion().getId(),
                        a.getQuestion().getQuestionText(),
                        a.getQuestion().getQuestionType(),
                        a.getAnswerValue(),
                        a.getCreatedAt()
                ))
                .toList();

        return new SurveySubmissionResponse(
                entity.getId(),
                entity.getStudentName(),
                entity.getStudentEmail(),
                entity.getFreeTextDescription(),
                entity.getSubmittedAt(),
                answers
        );
    }
}
