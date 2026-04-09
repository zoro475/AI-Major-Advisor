package com.example.prj.service;

import com.example.prj.dto.recommendation.CompareResponse;
import com.example.prj.dto.recommendation.RecommendationItemResponse;
import com.example.prj.dto.recommendation.RecommendationResponse;
import com.example.prj.dto.recommendation.RoadmapResponse;
import com.example.prj.dto.recommendation.ScorecardResponse;
import com.example.prj.entity.*;
import com.example.prj.repository.MajorRepository;
import com.example.prj.repository.RecommendationResultRepository;
import com.example.prj.repository.SurveySubmissionRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecommendationService {

    private final GeminiAiService geminiAiService;
    private final SurveySubmissionRepository submissionRepo;
    private final MajorRepository majorRepo;
    private final RecommendationResultRepository resultRepo;
    private final ObjectMapper objectMapper;

    // ==================== System Prompt ====================
    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên gia tư vấn hướng nghiệp cao cấp tại FPT Polytechnic, một trường cao đẳng hàng đầu Việt Nam.
            Nhiệm vụ của bạn là phân tích câu trả lời khảo sát của học sinh và đề xuất 3-5 ngành học phù hợp nhất.
            
            ## NGUYÊN TẮC PHÂN TÍCH
            
            1. **Phân tích đa chiều**: Đánh giá qua 5 yếu tố:
               - Sở thích cá nhân (thích làm gì, quan tâm lĩnh vực nào)
               - Tính cách & phong cách làm việc (hướng ngoại/nội, sáng tạo/logic, độc lập/nhóm)
               - Năng lực học thuật (môn giỏi, kỹ năng nổi bật)
               - Mục tiêu nghề nghiệp (muốn làm gì sau khi ra trường)
               - Xu hướng thị trường (ngành hot, mức lương, cơ hội việc làm)
            
            2. **Tính điểm phù hợp (matchScore)**:
               - 90-100%: Cực kỳ phù hợp — sở thích, năng lực, mục tiêu đều trùng khớp
               - 75-89%: Rất phù hợp — đa phần các yếu tố đều khớp
               - 60-74%: Phù hợp — có tiềm năng phát triển tốt
               - Dưới 60%: Không đưa vào kết quả
            
            3. **Lý do đề xuất**: Viết bằng tiếng Việt, giọng trẻ trung, thân thiện, dễ hiểu với học sinh cấp 3. Mỗi lý do phải:
               - Liên kết trực tiếp với câu trả lời của học sinh
               - Nêu rõ vì sao ngành này phù hợp với EM (không nói chung chung)
               - Đề cập cơ hội nghề nghiệp cụ thể và mức lương tham khảo
               - Độ dài 3-5 câu
            
            4. **Skills gap**: Với mỗi ngành, chỉ ra 1-2 kỹ năng học sinh cần phát triển thêm
            
            ## DANH SÁCH NGÀNH HỌC CÓ SẴN
            
            Chỉ đề xuất trong danh sách ngành học được cung cấp bên dưới. Không bịa ra ngành không có.
            
            {MAJORS_DATA}
            
            ## FORMAT OUTPUT
            
            Trả về JSON hợp lệ theo đúng cấu trúc sau:
            {
              "summary": "Tóm tắt ngắn gọn (2-3 câu) về profile học sinh và xu hướng nghề nghiệp",
              "recommendations": [
                {
                  "majorId": <ID ngành trong database>,
                  "majorName": "<Tên ngành>",
                  "fieldName": "<Tên lĩnh vực>",
                  "matchScore": <số nguyên 60-100>,
                  "reason": "<Lý do chi tiết vì sao phù hợp với học sinh>",
                  "careerPaths": ["<Nghề 1>", "<Nghề 2>", "<Nghề 3>"],
                  "salaryRange": "<Mức lương tham khảo>",
                  "skillsToImprove": ["<Kỹ năng cần phát triển 1>", "<Kỹ năng 2>"]
                }
              ]
            }
            
            Sắp xếp theo matchScore giảm dần. Đề xuất tối thiểu 3, tối đa 5 ngành.
            """;

    // ==================== Main Logic ====================

    /**
     * Phân tích khảo sát và đề xuất ngành học.
     * Nếu đã có kết quả trước đó, trả về cached.
     */
    @Transactional
    public RecommendationResponse analyze(Long submissionId) {
        // 1. Check cached result
        Optional<RecommendationResult> cached = resultRepo.findBySubmissionIdWithItems(submissionId);
        if (cached.isPresent()) {
            log.info("Trả về kết quả recommendation cached cho submissionId={}", submissionId);
            return toResponse(cached.get());
        }

        // 2. Load submission + answers
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        // 3. Load all majors
        List<Major> majors = majorRepo.findAll();
        if (majors.isEmpty()) {
            throw new RuntimeException("Chưa có dữ liệu ngành học trong database");
        }

        // 4. Build prompts
        String majorsData = buildMajorsDataPrompt(majors);
        String systemPrompt = SYSTEM_PROMPT.replace("{MAJORS_DATA}", majorsData);
        String userPrompt = buildStudentDataPrompt(submission);

        // 5. Call AI
        long startTime = System.currentTimeMillis();
        String aiResponse = geminiAiService.generate(systemPrompt, userPrompt);
        long processingTime = System.currentTimeMillis() - startTime;

        log.info("AI xử lý xong trong {}ms cho submissionId={}", processingTime, submissionId);

        // 6. Parse AI response
        RecommendationResult result = parseAndSaveResult(submission, aiResponse, processingTime, majors);

        return toResponse(result);
    }

    /**
     * Lấy kết quả recommendation đã lưu.
     */
    @Transactional(readOnly = true)
    public RecommendationResponse getBySubmissionId(Long submissionId) {
        RecommendationResult result = resultRepo.findBySubmissionIdWithItems(submissionId)
                .orElseThrow(() -> new RuntimeException(
                        "Chưa có kết quả phân tích cho bài khảo sát ID: " + submissionId));
        return toResponse(result);
    }

    // ==================== Compare Logic ====================

    private static final String COMPARE_SYSTEM_PROMPT = """
            Bạn là chuyên gia tư vấn hướng nghiệp cao cấp tại FPT Polytechnic.
            Nhiệm vụ: So sánh chi tiết 2 ngành học và đưa ra kết luận ngành nào phù hợp hơn với học sinh.
            
            ## NGUYÊN TẮC SO SÁNH
            
            1. So sánh CÔNG BẰNG, dựa trên DỮ LIỆU CỤ THỂ từ câu trả lời của học sinh
            2. Đánh giá theo 6 tiêu chí:
               - Sở thích & Đam mê: Ngành nào khớp hơn với sở thích
               - Năng lực & Tố chất: Năng lực hiện tại phù hợp ngành nào hơn
               - Cơ hội việc làm: Thị trường lao động, nhu cầu tuyển dụng
               - Thu nhập tiềm năng: Mức lương khi ra trường và sau 3-5 năm
               - Khả năng kháng AI: Ngành nào ít bị AI thay thế hơn
               - Lộ trình phát triển: Cơ hội thăng tiến lâu dài
            3. Kết luận phải RÕ RÀNG — chọn 1 ngành và giải thích vì sao
            4. Giọng văn thân thiện, dễ hiểu với học sinh cấp 3
            
            ## FORMAT OUTPUT
            
            Trả về JSON hợp lệ:
            {
              "major1": {
                "matchScore": <60-100>,
                "reason": "<Lý do phù hợp/không phù hợp, 3-4 câu>",
                "careerPaths": ["Nghề 1", "Nghề 2", "Nghề 3"],
                "keySubjects": ["Môn 1", "Môn 2", "Môn 3"],
                "skillsToImprove": ["Kỹ năng 1", "Kỹ năng 2"],
                "studyDuration": "2 năm / 2.5 năm / 3 năm",
                "admissionRequirements": "Yêu cầu đầu vào"
              },
              "major2": {
                "matchScore": <60-100>,
                "reason": "<Lý do phù hợp/không phù hợp, 3-4 câu>",
                "careerPaths": ["Nghề 1", "Nghề 2", "Nghề 3"],
                "keySubjects": ["Môn 1", "Môn 2", "Môn 3"],
                "skillsToImprove": ["Kỹ năng 1", "Kỹ năng 2"],
                "studyDuration": "2 năm / 2.5 năm / 3 năm",
                "admissionRequirements": "Yêu cầu đầu vào"
              },
              "conclusion": "<Kết luận 4-6 câu: AI chọn ngành nào và giải thích chi tiết vì sao dựa trên profile học sinh>",
              "conclusionMajorId": <ID ngành được recommend>
            }
            """;

    /**
     * So sánh 2 ngành học dựa trên profile học sinh.
     */
    @Transactional(readOnly = true)
    public CompareResponse compare(Long submissionId, Long majorId1, Long majorId2) {
        // 1. Load submission
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        // 2. Load 2 majors
        Major major1 = majorRepo.findById(majorId1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId1));
        Major major2 = majorRepo.findById(majorId2)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId2));

        // 3. Build user prompt
        String studentData = buildStudentDataPrompt(submission);
        String userPrompt = studentData + "\n\n## HAI NGÀNH CẦN SO SÁNH\n\n"
                + buildSingleMajorPrompt(major1, "Ngành 1")
                + "\n"
                + buildSingleMajorPrompt(major2, "Ngành 2");

        // 4. Call AI
        String aiResponse = geminiAiService.generate(COMPARE_SYSTEM_PROMPT, userPrompt);

        // 5. Parse response
        return parseCompareResponse(submission, major1, major2, aiResponse);
    }

    private String buildSingleMajorPrompt(Major m, String label) {
        StringBuilder sb = new StringBuilder();
        sb.append("### ").append(label).append("\n");
        sb.append("ID: ").append(m.getId()).append("\n");
        sb.append("Tên: ").append(m.getName()).append("\n");
        if (m.getField() != null) sb.append("Lĩnh vực: ").append(m.getField().getName()).append("\n");
        if (m.getTarget() != null) sb.append("Mô tả: ").append(m.getTarget()).append("\n");
        if (m.getCareerOpportunities() != null) sb.append("Cơ hội nghề nghiệp: ").append(m.getCareerOpportunities()).append("\n");
        sb.append("Lương: ").append(m.getSalaryRange() != null ? m.getSalaryRange() : "N/A").append("\n");
        sb.append("Độ hot: ").append(m.getHotTrendScore() != null ? m.getHotTrendScore() : 5).append("/10\n");
        sb.append("Kháng AI: ").append(m.getAiResistanceScore() != null ? m.getAiResistanceScore() : 5).append("/10\n");
        return sb.toString();
    }

    private CompareResponse parseCompareResponse(
            SurveySubmission submission, Major major1, Major major2, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            JsonNode m1 = root.path("major1");
            JsonNode m2 = root.path("major2");

            CompareResponse.MajorCompareDetail detail1 = buildCompareDetail(
                    major1, m1);
            CompareResponse.MajorCompareDetail detail2 = buildCompareDetail(
                    major2, m2);

            Long conclusionId = root.path("conclusionMajorId").asLong(major1.getId());
            String conclusionName = conclusionId.equals(major1.getId()) ? major1.getName() : major2.getName();

            return new CompareResponse(
                    submission.getId(),
                    submission.getStudentName(),
                    detail1,
                    detail2,
                    root.path("conclusion").asText(""),
                    conclusionName,
                    conclusionId
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse compare response: {}", aiResponse, e);
            throw new RuntimeException("Lỗi xử lý phản hồi so sánh AI", e);
        }
    }

    private CompareResponse.MajorCompareDetail buildCompareDetail(Major major, JsonNode node) {
        return new CompareResponse.MajorCompareDetail(
                major.getId(),
                major.getName(),
                major.getField() != null ? major.getField().getName() : "",
                node.path("matchScore").asInt(70),
                node.path("reason").asText(""),
                parseJsonArrayNode(node.has("careerPaths") ? node.get("careerPaths").toString() : "[]"),
                major.getSalaryRange() != null ? major.getSalaryRange() : node.path("salaryRange").asText(""),
                major.getHotTrendScore() != null ? major.getHotTrendScore() : 5,
                major.getAiResistanceScore() != null ? major.getAiResistanceScore() : 5,
                parseJsonArrayNode(node.has("keySubjects") ? node.get("keySubjects").toString() : "[]"),
                parseJsonArrayNode(node.has("skillsToImprove") ? node.get("skillsToImprove").toString() : "[]"),
                node.path("studyDuration").asText("2.5 năm"),
                node.path("admissionRequirements").asText("Tốt nghiệp THPT")
        );
    }

    // ==================== Roadmap Logic ====================

    private static final String ROADMAP_SYSTEM_PROMPT = """
            Bạn là chuyên gia tư vấn lộ trình nghề nghiệp tại FPT Polytechnic.
            Nhiệm vụ: Tạo lộ trình phát triển nghề nghiệp CHI TIẾT cho ngành học được chọn, phù hợp với profile học sinh.
            
            ## NGUYÊN TẮC
            
            1. Chia thành 4 giai đoạn rõ ràng: Fresher → Junior → Mid-level → Senior
            2. Mỗi giai đoạn cần: kỹ năng cụ thể, mức lương thực tế tại VN, vị trí ứng tuyển
            3. Liệt kê chứng chỉ có giá trị thực tế (quốc tế và trong nước)
            4. Đề xuất 3-4 mảng việc làm cụ thể với công cụ/công nghệ liên quan
            5. Giọng văn thân thiện, dễ hiểu với học sinh cấp 3
            6. Dữ liệu lương và xu hướng dựa trên thị trường Việt Nam 2025-2030
            
            ## FORMAT OUTPUT
            
            Trả về JSON hợp lệ:
            {
              "overview": "Tóm tắt 2-3 câu về triển vọng ngành",
              "phases": [
                {
                  "title": "Giai đoạn 1: Fresher",
                  "period": "0-1 năm",
                  "description": "Mô tả 2-3 câu",
                  "skills": ["Kỹ năng 1", "Kỹ năng 2"],
                  "salaryRange": "8-12 triệu",
                  "positions": ["Vị trí 1", "Vị trí 2"]
                }
              ],
              "certifications": ["Chứng chỉ 1", "Chứng chỉ 2"],
              "careerBranches": [
                {
                  "name": "Tên mảng",
                  "description": "Mô tả 1-2 câu",
                  "tools": ["Tool 1", "Tool 2"],
                  "demandLevel": "Rất cao"
                }
              ]
            }
            
            Tạo đúng 4 phases và 3-4 careerBranches.
            """;

    @Transactional(readOnly = true)
    public RoadmapResponse getRoadmap(Long submissionId, Long majorId) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));
        Major major = majorRepo.findById(majorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId));

        String studentData = buildStudentDataPrompt(submission);
        String userPrompt = studentData + "\n\n## NGÀNH HỌC ĐÃ CHỌN\n\n" + buildSingleMajorPrompt(major, "Ngành");

        String aiResponse = geminiAiService.generate(ROADMAP_SYSTEM_PROMPT, userPrompt);
        return parseRoadmapResponse(major, aiResponse);
    }

    private RoadmapResponse parseRoadmapResponse(Major major, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            List<RoadmapResponse.RoadmapPhase> phases = new ArrayList<>();
            for (JsonNode p : root.path("phases")) {
                phases.add(new RoadmapResponse.RoadmapPhase(
                        p.path("title").asText(),
                        p.path("period").asText(),
                        p.path("description").asText(),
                        parseJsonArrayNode(p.has("skills") ? p.get("skills").toString() : "[]"),
                        p.path("salaryRange").asText(),
                        parseJsonArrayNode(p.has("positions") ? p.get("positions").toString() : "[]")
                ));
            }

            List<RoadmapResponse.CareerBranch> branches = new ArrayList<>();
            for (JsonNode b : root.path("careerBranches")) {
                branches.add(new RoadmapResponse.CareerBranch(
                        b.path("name").asText(),
                        b.path("description").asText(),
                        parseJsonArrayNode(b.has("tools") ? b.get("tools").toString() : "[]"),
                        b.path("demandLevel").asText("Cao")
                ));
            }

            return new RoadmapResponse(
                    major.getId(),
                    major.getName(),
                    root.path("overview").asText(),
                    phases,
                    parseJsonArrayNode(root.has("certifications") ? root.get("certifications").toString() : "[]"),
                    branches
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse roadmap response", e);
            throw new RuntimeException("Lỗi xử lý lộ trình AI", e);
        }
    }

    // ==================== Scorecard Logic ====================

    private static final String SCORECARD_SYSTEM_PROMPT = """
            Bạn là chuyên gia phân tích xu hướng ngành nghề và thị trường lao động Việt Nam.
            Nhiệm vụ: Chấm điểm ngành học theo 6 chỉ số "Future-Proof" dựa trên profile học sinh.
            
            ## 6 CHỈ SỐ ĐÁNH GIÁ
            
            1. **Phù hợp cá nhân** (personalFit): Mức độ ngành khớp với sở thích, tính cách, năng lực của học sinh
            2. **Kháng AI Automation** (aiResistance): Khả năng ngành này KHÔNG bị AI/robot thay thế trong 10 năm tới
            3. **Tác động xã hội & Bền vững** (socialImpact): Ngành có đóng góp tích cực cho xã hội, môi trường không
            4. **Thu nhập & Tăng trưởng** (incomeGrowth): Mức lương khởi điểm + tốc độ tăng lương sau 5-10 năm
            5. **Linh hoạt chuyển ngành** (flexibility): Kỹ năng có transferable không, dễ pivot sang ngành khác
            6. **Độ hot VN 2026-2035** (marketDemand): Nhu cầu tuyển dụng tại Việt Nam trong thập kỷ tới
            
            ## QUY TẮC CHẤM ĐIỂM
            
            - Mỗi chỉ số: 0-100 điểm
            - 85-100: Xuất sắc | 70-84: Tốt | 55-69: Khá | 40-54: Trung bình | Dưới 40: Yếu
            - Điểm tổng (overall) = trung bình có trọng số (personalFit x1.5, incomeGrowth x1.2, các chỉ số khác x1)
            - Giải thích ngắn (1-2 câu) cho mỗi chỉ số, liên kết với profile học sinh
            - Nhận xét tổng quan 3-4 câu, cụ thể và có tính xây dựng
            
            ## FORMAT OUTPUT
            
            Trả về JSON hợp lệ:
            {
              "overallScore": <0-100>,
              "overallVerdict": "Xuất sắc / Tốt / Khá / Trung bình",
              "metrics": [
                {
                  "name": "Phù hợp cá nhân",
                  "icon": "🎯",
                  "score": <0-100>,
                  "label": "Xuất sắc / Tốt / Khá / Trung bình / Yếu",
                  "explanation": "Giải thích 1-2 câu"
                },
                {
                  "name": "Kháng AI Automation",
                  "icon": "🛡️",
                  "score": <0-100>,
                  "label": "...",
                  "explanation": "..."
                },
                {
                  "name": "Tác động xã hội",
                  "icon": "🌍",
                  "score": <0-100>,
                  "label": "...",
                  "explanation": "..."
                },
                {
                  "name": "Thu nhập & Tăng trưởng",
                  "icon": "💰",
                  "score": <0-100>,
                  "label": "...",
                  "explanation": "..."
                },
                {
                  "name": "Linh hoạt chuyển ngành",
                  "icon": "🔄",
                  "score": <0-100>,
                  "label": "...",
                  "explanation": "..."
                },
                {
                  "name": "Độ hot VN 2026-2035",
                  "icon": "🔥",
                  "score": <0-100>,
                  "label": "...",
                  "explanation": "..."
                }
              ],
              "aiInsight": "Nhận xét tổng quan 3-4 câu"
            }
            
            Luôn trả về đúng 6 metrics theo thứ tự trên.
            """;

    @Transactional(readOnly = true)
    public ScorecardResponse getScorecard(Long submissionId, Long majorId) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));
        Major major = majorRepo.findById(majorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId));

        String studentData = buildStudentDataPrompt(submission);
        String userPrompt = studentData + "\n\n## NGÀNH CẦN ĐÁNH GIÁ\n\n" + buildSingleMajorPrompt(major, "Ngành");

        String aiResponse = geminiAiService.generate(SCORECARD_SYSTEM_PROMPT, userPrompt);
        return parseScorecardResponse(major, aiResponse);
    }

    private ScorecardResponse parseScorecardResponse(Major major, String aiResponse) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            List<ScorecardResponse.ScoreMetric> metrics = new ArrayList<>();
            for (JsonNode m : root.path("metrics")) {
                metrics.add(new ScorecardResponse.ScoreMetric(
                        m.path("name").asText(),
                        m.path("icon").asText("📊"),
                        m.path("score").asInt(50),
                        m.path("label").asText("Khá"),
                        m.path("explanation").asText("")
                ));
            }

            return new ScorecardResponse(
                    major.getId(),
                    major.getName(),
                    root.path("overallScore").asInt(70),
                    root.path("overallVerdict").asText("Khá"),
                    metrics,
                    root.path("aiInsight").asText("")
            );
        } catch (JsonProcessingException e) {
            log.error("Lỗi parse scorecard response", e);
            throw new RuntimeException("Lỗi xử lý scorecard AI", e);
        }
    }

    // ==================== Prompt Building ====================

    /**
     * Chuyển câu trả lời học sinh thành text prompt.
     */
    private String buildStudentDataPrompt(SurveySubmission submission) {
        StringBuilder sb = new StringBuilder();
        sb.append("## THÔNG TIN HỌC SINH\n\n");
        sb.append("Tên: ").append(submission.getStudentName()).append("\n\n");

        sb.append("### Câu trả lời khảo sát:\n");
        for (SurveyAnswer answer : submission.getAnswers()) {
            SurveyQuestion q = answer.getQuestion();
            sb.append("- **").append(q.getQuestionText()).append("**: ");

            switch (q.getQuestionType()) {
                case SINGLE_CHOICE -> sb.append(answer.getAnswerValue());
                case MULTIPLE_CHOICE -> {
                    List<String> values = parseJsonArray(answer.getAnswerValue());
                    sb.append(String.join(", ", values));
                }
                case RATING -> sb.append(answer.getAnswerValue())
                        .append("/").append(q.getMaxRating() != null ? q.getMaxRating() : 5).append(" sao");
                case TEXTAREA -> sb.append(answer.getAnswerValue());
            }
            sb.append("\n");
        }

        if (submission.getFreeTextDescription() != null && !submission.getFreeTextDescription().isBlank()) {
            sb.append("\n### Mô tả thêm từ học sinh:\n");
            sb.append(submission.getFreeTextDescription()).append("\n");
        }

        return sb.toString();
    }

    /**
     * Chuyển danh sách ngành học thành text prompt.
     */
    private String buildMajorsDataPrompt(List<Major> majors) {
        StringBuilder sb = new StringBuilder();
        for (Major m : majors) {
            sb.append("---\n");
            sb.append("ID: ").append(m.getId()).append("\n");
            sb.append("Ngành: ").append(m.getName()).append("\n");
            if (m.getField() != null) {
                sb.append("Lĩnh vực: ").append(m.getField().getName()).append("\n");
            }
            if (m.getTarget() != null && !m.getTarget().isBlank()) {
                String target = m.getTarget();
                sb.append("Mô tả: ").append(target, 0, Math.min(300, target.length()));
                if (target.length() > 300) sb.append("...");
                sb.append("\n");
            }
            if (m.getCareerOpportunities() != null && !m.getCareerOpportunities().isBlank()) {
                String career = m.getCareerOpportunities();
                sb.append("Cơ hội nghề nghiệp: ").append(career, 0, Math.min(300, career.length()));
                if (career.length() > 300) sb.append("...");
                sb.append("\n");
            }
            if (m.getSalaryRange() != null) {
                sb.append("Lương: ").append(m.getSalaryRange()).append("\n");
            }
            sb.append("Độ hot: ").append(m.getHotTrendScore() != null ? m.getHotTrendScore() : 5).append("/10\n");
            sb.append("Kháng AI: ").append(m.getAiResistanceScore() != null ? m.getAiResistanceScore() : 5).append("/10\n");
        }
        return sb.toString();
    }

    // ==================== Response Parsing ====================

    /**
     * Parse AI JSON response và lưu vào DB.
     */
    private RecommendationResult parseAndSaveResult(
            SurveySubmission submission, String aiResponse, long processingTime, List<Major> majors) {

        // Map majorId → Major entity để lookup
        Map<Long, Major> majorMap = majors.stream()
                .collect(Collectors.toMap(Major::getId, m -> m));

        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            RecommendationResult result = RecommendationResult.builder()
                    .submission(submission)
                    .aiSummary(root.path("summary").asText(""))
                    .aiRawResponse(aiResponse)
                    .modelUsed(geminiAiService.getModel())
                    .processingTimeMs(processingTime)
                    .build();

            JsonNode recommendations = root.path("recommendations");
            if (recommendations.isArray()) {
                int order = 0;
                for (JsonNode rec : recommendations) {
                    Long majorId = rec.path("majorId").asLong(0);
                    Major major = majorMap.get(majorId);

                    // Fallback: nếu AI trả majorId không đúng, tìm theo tên
                    if (major == null) {
                        String majorName = rec.path("majorName").asText("");
                        major = majors.stream()
                                .filter(m -> m.getName().equalsIgnoreCase(majorName))
                                .findFirst()
                                .orElse(null);
                    }

                    if (major == null) {
                        log.warn("Bỏ qua recommendation: majorId={} không tìm thấy", majorId);
                        continue;
                    }

                    RecommendationItem item = RecommendationItem.builder()
                            .result(result)
                            .major(major)
                            .majorName(rec.path("majorName").asText(major.getName()))
                            .fieldName(rec.path("fieldName").asText(
                                    major.getField() != null ? major.getField().getName() : ""))
                            .matchScore(rec.path("matchScore").asInt(70))
                            .reason(rec.path("reason").asText(""))
                            .careerPaths(rec.has("careerPaths") ? rec.get("careerPaths").toString() : "[]")
                            .salaryRange(rec.path("salaryRange").asText(""))
                            .skillsToImprove(rec.has("skillsToImprove") ? rec.get("skillsToImprove").toString() : "[]")
                            .displayOrder(order++)
                            .build();

                    result.getItems().add(item);
                }
            }

            result = resultRepo.save(result);
            log.info("Lưu {} recommendation items cho submissionId={}", result.getItems().size(), submission.getId());
            return result;

        } catch (JsonProcessingException e) {
            log.error("Lỗi parse AI response JSON: {}", aiResponse, e);
            throw new RuntimeException("Lỗi xử lý phản hồi từ AI", e);
        }
    }

    // ==================== Helper Methods ====================

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return List.of(json);
        }
    }

    private List<String> parseJsonArrayNode(String json) {
        if (json == null || json.isBlank() || json.equals("[]")) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }

    private RecommendationResponse toResponse(RecommendationResult result) {
        List<RecommendationItemResponse> items = result.getItems().stream()
                .sorted(Comparator.comparingInt(RecommendationItem::getDisplayOrder))
                .map(item -> new RecommendationItemResponse(
                        item.getMajor().getId(),
                        item.getMajorName(),
                        item.getFieldName(),
                        item.getMatchScore(),
                        item.getReason(),
                        parseJsonArrayNode(item.getCareerPaths()),
                        item.getSalaryRange(),
                        parseJsonArrayNode(item.getSkillsToImprove())
                ))
                .toList();

        return new RecommendationResponse(
                result.getId(),
                result.getSubmission().getId(),
                result.getSubmission().getStudentName(),
                result.getAiSummary(),
                result.getModelUsed(),
                result.getProcessingTimeMs(),
                result.getCreatedAt(),
                items
        );
    }
}
