package com.example.prj.service;

import com.example.prj.dto.recommendation.CompareResponse;
import com.example.prj.dto.recommendation.RecommendationItemResponse;
import com.example.prj.dto.recommendation.RecommendationResponse;
import com.example.prj.dto.recommendation.RoadmapResponse;
import com.example.prj.dto.recommendation.ScorecardResponse;
import com.example.prj.dto.recommendation.TimeMachineResponse;
import com.example.prj.dto.recommendation.WhatIfResponse;
import com.example.prj.dto.recommendation.HybridCareerResponse;
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
            
            ⚠️ BẮT BUỘC: Bạn CHỈ được chọn ngành từ danh sách dưới đây.
            - majorId: PHẢI dùng đúng số ID trong danh sách
            - majorName: PHẢI copy CHÍNH XÁC tên ngành (viết hoa, có dấu), KHÔNG được đổi tên hay viết tắt
            - Ví dụ: nếu danh sách có ID=5, Ngành="MARKETING & SALES" thì majorId=5, majorName="MARKETING & SALES"
            
            {MAJORS_DATA}
            
            ## FORMAT OUTPUT
            
            Trả về JSON hợp lệ theo đúng cấu trúc sau:
            {
              "summary": "Tóm tắt ngắn gọn (2-3 câu) về profile học sinh và xu hướng nghề nghiệp",
              "recommendations": [
                {
                  "majorId": <ID ngành CHÍNH XÁC từ danh sách trên>,
                  "majorName": "<Tên ngành CHÍNH XÁC từ danh sách trên>",
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
            if (!cached.get().getItems().isEmpty()) {
                log.info("Trả về kết quả recommendation cached cho submissionId={} ({} items)",
                        submissionId, cached.get().getItems().size());
                return toResponse(cached.get());
            } else {
                // Cache rỗng (lần trước AI parse fail) → xóa và phân tích lại
                log.info("Xóa cache rỗng cho submissionId={}, phân tích lại...", submissionId);
                resultRepo.delete(cached.get());
                resultRepo.flush();
            }
        }

        // 2. Load submission + answers
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        // 3. Load all majors
        List<Major> majors = majorRepo.findAll();
        if (majors.isEmpty()) {
            throw new RuntimeException("Chưa có dữ liệu ngành học trong database");
        }
        log.info("DB có {} ngành: {}", majors.size(),
                majors.stream().map(m -> m.getId() + "=" + m.getName()).collect(Collectors.joining(", ")));

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
            log.info("AI raw response (first 500 chars): {}", aiResponse.substring(0, Math.min(500, aiResponse.length())));

            RecommendationResult result = RecommendationResult.builder()
                    .submission(submission)
                    .aiSummary(root.path("summary").asText(""))
                    .aiRawResponse(aiResponse)
                    .modelUsed(geminiAiService.getModel())
                    .processingTimeMs(processingTime)
                    .build();

            JsonNode recommendations = root.path("recommendations");
            log.info("Recommendations array found: {}, size: {}", recommendations.isArray(), recommendations.isArray() ? recommendations.size() : 0);
            if (recommendations.isArray()) {
                int order = 0;
                for (JsonNode rec : recommendations) {
                    Long majorId = rec.path("majorId").asLong(0);
                    String recName = rec.path("majorName").asText("");
                    log.info("Processing recommendation: majorId={}, majorName='{}'", majorId, recName);
                    Major major = majorMap.get(majorId);

                    // Fallback 1: tìm theo tên chính xác
                    if (major == null) {
                        String majorName = rec.path("majorName").asText("");
                        major = majors.stream()
                                .filter(m -> m.getName().equalsIgnoreCase(majorName))
                                .findFirst()
                                .orElse(null);

                        // Fallback 2: tìm theo tên chứa (partial match)
                        if (major == null && !majorName.isBlank()) {
                            String searchName = majorName.toUpperCase();
                            major = majors.stream()
                                    .filter(m -> m.getName().toUpperCase().contains(searchName)
                                            || searchName.contains(m.getName().toUpperCase()))
                                    .findFirst()
                                    .orElse(null);
                        }

                        // Fallback 3: tìm theo độ tương đồng từ (word similarity)
                        if (major == null && !majorName.isBlank()) {
                            major = findBestMatchByWords(majorName, majors);
                        }

                        if (major != null && !major.getName().equalsIgnoreCase(majorName)) {
                            log.info("Fuzzy matched: AI='{}' → DB='{}'", majorName, major.getName());
                        }
                    }

                    if (major == null) {
                        log.warn("Bỏ qua recommendation: majorId={}, majorName='{}' không tìm thấy trong DB",
                                majorId, rec.path("majorName").asText(""));
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

    // ==================== Fuzzy Matching ====================

    /**
     * Tìm ngành phù hợp nhất bằng so sánh từ chung.
     * VD: "QUẢN LÝ KHÁCH SẠN" vs "QUẢN TRỊ KHÁCH SẠN NHÀ HÀNG" → 2/4 từ chung = 50%
     * "KINH DOANH - MARKETING" vs "MARKETING & SALES" → 1/3 từ chung (MARKETING)
     */
    private Major findBestMatchByWords(String aiName, List<Major> majors) {
        Set<String> aiWords = extractWords(aiName);
        if (aiWords.isEmpty()) return null;

        Major bestMatch = null;
        double bestScore = 0;

        for (Major m : majors) {
            Set<String> dbWords = extractWords(m.getName());
            if (dbWords.isEmpty()) continue;

            // Đếm số từ chung
            long commonWords = aiWords.stream().filter(dbWords::contains).count();
            // Tính similarity = từ chung / min(số từ AI, số từ DB)
            double similarity = (double) commonWords / Math.min(aiWords.size(), dbWords.size());

            if (similarity > bestScore && similarity >= 0.4) {
                bestScore = similarity;
                bestMatch = m;
            }
        }

        return bestMatch;
    }

    private Set<String> extractWords(String name) {
        if (name == null || name.isBlank()) return Collections.emptySet();
        // Tách theo khoảng trắng, -, &, loại bỏ ký tự đặc biệt
        return Arrays.stream(name.toUpperCase().split("[\\s\\-&/]+"))
                .map(String::trim)
                .filter(w -> !w.isBlank() && w.length() > 1)
                .collect(Collectors.toSet());
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

    // ==================== Time Machine ====================

    private static final String TIME_MACHINE_SYSTEM_PROMPT = """
            Bạn là "Cỗ máy thời gian nghề nghiệp" — giúp học sinh Việt Nam hình dung tương lai sống động nếu chọn ngành học này.

            ## NHIỆM VỤ
            Tạo 3 PHIÊN BẢN TƯƠNG LAI chi tiết, chân thực cho học sinh dựa trên profile và ngành đã chọn.

            ## 3 MỐC THỜI GIAN
            1. **5 năm sau** (emoji: 🌱, title dạng "Junior → Mid-level") — Giai đoạn khởi đầu, đang học hỏi
            2. **10 năm sau** (emoji: 🚀, title dạng "Senior / Team Lead") — Đã có kinh nghiệm, vị thế
            3. **15 năm sau** (emoji: 👑, title dạng "Manager / Expert") — Đỉnh cao sự nghiệp

            ## YÊU CẦU NỘI DUNG CHO MỖI SNAPSHOT

            ### dayInLife — Viết như TRUYỆN NGẮN, rất sống động:
            - morning: Mô tả buổi sáng (7h-12h) — em làm gì, ở đâu, dùng công cụ gì
            - afternoon: Buổi chiều (13h-17h) — công việc chính, họp hành, dự án
            - evening: Buổi tối (18h-22h) — học thêm gì, ở đâu, với ai
            - highlight: Khoảnh khắc đáng nhớ nhất trong ngày (1-2 câu)
            → Ngôi kể: "em". Bối cảnh: Việt Nam (công ty VN, TP.HCM/Hà Nội, tên công ty giả VN).

            ### achievements: 2-3 thành tựu cụ thể đã đạt được tại mốc đó
            ### challenges: 2-3 thách thức, mỗi cái có name, description, howToOvercome
            ### opportunities: 2-3 cơ hội nổi bật
            ### salaryRange: Mức lương dự kiến (VD: "25 - 40 triệu")

            ### worstCase — Điều gì xảy ra nếu em KHÔNG phát triển:
            - scenario: Mô tả chân thực (không đe dọa, giọng nhẹ nhàng cảnh báo)
            - consequences: Hậu quả cụ thể
            - preventionTip: Cách phòng tránh, 1-2 câu

            ## FORMAT OUTPUT
            Trả JSON hợp lệ:
            {
              "majorName": "Tên ngành",
              "studentProfile": "Tóm tắt 1-2 câu profile em",
              "snapshots": [
                {
                  "yearsFromNow": 5,
                  "title": "Fresher → Mid-level Developer",
                  "emoji": "🌱",
                  "dayInLife": { "morning": "...", "afternoon": "...", "evening": "...", "highlight": "..." },
                  "salaryRange": "15 - 25 triệu",
                  "achievements": ["...", "..."],
                  "challenges": [{ "name": "...", "description": "...", "howToOvercome": "..." }],
                  "opportunities": ["...", "..."],
                  "worstCase": { "scenario": "...", "consequences": "...", "preventionTip": "..." }
                }
              ],
              "overallMessage": "Lời nhắn khích lệ 2-3 câu"
            }

            PHẢI có đúng 3 snapshots (5, 10, 15 năm). Viết bằng tiếng Việt.
            """;

    public TimeMachineResponse getTimeMachine(Long submissionId, Long majorId,
                                              List<String> customSkills, List<String> customInterests) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        Major major = majorRepo.findById(majorId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId));

        String userPrompt = buildStudentDataPrompt(submission);
        userPrompt += "\n\n## NGÀNH ĐƯỢC CHỌN\n";
        userPrompt += "Ngành: " + major.getName() + "\n";
        if (major.getField() != null) {
            userPrompt += "Lĩnh vực: " + major.getField().getName() + "\n";
        }
        if (major.getCareerOpportunities() != null) {
            userPrompt += "Cơ hội: " + major.getCareerOpportunities().substring(0, Math.min(300, major.getCareerOpportunities().length())) + "\n";
        }

        // What-if mode
        if (customSkills != null && !customSkills.isEmpty()) {
            userPrompt += "\n## WHAT-IF: Kỹ năng bổ sung\n";
            userPrompt += "Em giả sử mình có thêm các kỹ năng: " + String.join(", ", customSkills) + "\n";
            userPrompt += "Hãy điều chỉnh kết quả dựa trên các kỹ năng mới này.\n";
        }
        if (customInterests != null && !customInterests.isEmpty()) {
            userPrompt += "\n## WHAT-IF: Sở thích thay đổi\n";
            userPrompt += "Em giả sử sở thích thay đổi thành: " + String.join(", ", customInterests) + "\n";
            userPrompt += "Hãy điều chỉnh kết quả dựa trên sở thích mới.\n";
        }

        long startTime = System.currentTimeMillis();
        String aiResponse = geminiAiService.generate(TIME_MACHINE_SYSTEM_PROMPT, userPrompt);
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Time Machine AI xử lý xong trong {}ms cho majorId={}", processingTime, majorId);

        return parseTimeMachineResponse(aiResponse, major.getName());
    }

    private TimeMachineResponse parseTimeMachineResponse(String aiResponse, String majorName) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);

            String studentProfile = root.path("studentProfile").asText("");
            String overallMessage = root.path("overallMessage").asText("Chúc em thành công trên hành trình sự nghiệp!");
            String name = root.path("majorName").asText(majorName);

            List<TimeMachineResponse.FutureSnapshot> snapshots = new ArrayList<>();
            JsonNode snapshotsNode = root.path("snapshots");
            if (snapshotsNode.isArray()) {
                for (JsonNode snap : snapshotsNode) {
                    JsonNode dil = snap.path("dayInLife");
                    var dayInLife = new TimeMachineResponse.DayInLife(
                            dil.path("morning").asText(""),
                            dil.path("afternoon").asText(""),
                            dil.path("evening").asText(""),
                            dil.path("highlight").asText("")
                    );

                    List<TimeMachineResponse.Challenge> challenges = new ArrayList<>();
                    JsonNode chNode = snap.path("challenges");
                    if (chNode.isArray()) {
                        for (JsonNode c : chNode) {
                            challenges.add(new TimeMachineResponse.Challenge(
                                    c.path("name").asText(""),
                                    c.path("description").asText(""),
                                    c.path("howToOvercome").asText("")
                            ));
                        }
                    }

                    JsonNode wc = snap.path("worstCase");
                    var worstCase = new TimeMachineResponse.WorstCase(
                            wc.path("scenario").asText(""),
                            wc.path("consequences").asText(""),
                            wc.path("preventionTip").asText("")
                    );

                    snapshots.add(new TimeMachineResponse.FutureSnapshot(
                            snap.path("yearsFromNow").asInt(5),
                            snap.path("title").asText(""),
                            snap.path("emoji").asText("🌱"),
                            dayInLife,
                            snap.path("salaryRange").asText(""),
                            parseJsonArrayNode(snap.has("achievements") ? snap.get("achievements").toString() : "[]"),
                            challenges,
                            parseJsonArrayNode(snap.has("opportunities") ? snap.get("opportunities").toString() : "[]"),
                            worstCase
                    ));
                }
            }

            return new TimeMachineResponse(name, studentProfile, snapshots, overallMessage);
        } catch (Exception e) {
            log.error("Lỗi parse Time Machine response: {}", aiResponse, e);
            throw new RuntimeException("Lỗi xử lý Time Machine response", e);
        }
    }

    // ==================== What-If Simulator ====================

    private static final String WHAT_IF_SYSTEM_PROMPT = """
            Bạn là hệ thống mô phỏng What-If nghề nghiệp tại FPT Polytechnic.

            ## NHIỆM VỤ
            So sánh KẾT QUẢ GỐC với KẾT QUẢ MỚI khi học sinh thay đổi kỹ năng/sở thích.
            Phân tích sự thay đổi matchScore cho từng ngành.

            ## DANH SÁCH NGÀNH
            {MAJORS_DATA}

            ## YÊU CẦU
            - Với mỗi ngành, đánh giá matchScore mới (0-100)
            - Tính scoreDelta = newScore - originalScore
            - changeReason: 1-2 câu giải thích vì sao score thay đổi
            - aiAnalysis: Phân tích tổng thể 3-4 câu về tác động của thay đổi

            ⚠️ BẮT BUỘC: majorId và majorName PHẢI chính xác từ danh sách trên.

            ## FORMAT: JSON hợp lệ
            {
              "originalProfile": "Tóm tắt profile gốc 1-2 câu",
              "whatIfProfile": "Tóm tắt profile mới sau thay đổi",
              "changes": [
                { "majorId": <ID>, "majorName": "...", "originalScore": <0-100>, "newScore": <0-100>, "scoreDelta": <+/- số>, "changeReason": "..." }
              ],
              "aiAnalysis": "Phân tích tổng thể..."
            }
            Sắp xếp theo |scoreDelta| giảm dần (thay đổi lớn nhất trước). Liệt kê top 5-8 ngành.
            """;

    public WhatIfResponse getWhatIf(Long submissionId, List<String> addSkills,
                                    List<String> removeSkills, List<String> newInterests,
                                    String newPersonality) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        List<Major> majors = majorRepo.findAll();
        String majorsData = buildMajorsDataPrompt(majors);
        String systemPrompt = WHAT_IF_SYSTEM_PROMPT.replace("{MAJORS_DATA}", majorsData);

        String userPrompt = buildStudentDataPrompt(submission);
        userPrompt += "\n\n## THAY ĐỔI WHAT-IF\n";
        if (addSkills != null && !addSkills.isEmpty())
            userPrompt += "Thêm kỹ năng: " + String.join(", ", addSkills) + "\n";
        if (removeSkills != null && !removeSkills.isEmpty())
            userPrompt += "Bỏ kỹ năng: " + String.join(", ", removeSkills) + "\n";
        if (newInterests != null && !newInterests.isEmpty())
            userPrompt += "Sở thích mới: " + String.join(", ", newInterests) + "\n";
        if (newPersonality != null && !newPersonality.isBlank())
            userPrompt += "Tính cách thay đổi: " + newPersonality + "\n";

        long startTime = System.currentTimeMillis();
        String aiResponse = geminiAiService.generate(systemPrompt, userPrompt);
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("What-If AI xử lý xong trong {}ms", processingTime);

        return parseWhatIfResponse(aiResponse, submission.getStudentName());
    }

    private WhatIfResponse parseWhatIfResponse(String aiResponse, String studentName) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            List<WhatIfResponse.MajorChange> changes = new ArrayList<>();
            JsonNode changesNode = root.path("changes");
            if (changesNode.isArray()) {
                for (JsonNode c : changesNode) {
                    changes.add(new WhatIfResponse.MajorChange(
                            c.path("majorId").asLong(0),
                            c.path("majorName").asText(""),
                            c.path("originalScore").asInt(0),
                            c.path("newScore").asInt(0),
                            c.path("scoreDelta").asInt(0),
                            c.path("changeReason").asText("")
                    ));
                }
            }
            return new WhatIfResponse(
                    studentName,
                    root.path("originalProfile").asText(""),
                    root.path("whatIfProfile").asText(""),
                    changes,
                    root.path("aiAnalysis").asText("")
            );
        } catch (Exception e) {
            log.error("Lỗi parse What-If response: {}", aiResponse, e);
            throw new RuntimeException("Lỗi xử lý What-If response", e);
        }
    }

    // ==================== Hybrid Career ====================

    private static final String HYBRID_CAREER_SYSTEM_PROMPT = """
            Bạn là chuyên gia tư vấn nghề nghiệp HYBRID — kết hợp 2 ngành thành nghề nghiệp mới.

            ## NHIỆM VỤ
            Học sinh quan tâm cả \"{major1}\" và \"{major2}\". Tìm 3-5 nghề HYBRID kết hợp cả 2 ngành.

            ## YÊU CẦU
            - Nghề phải THỰC SỰ TỒN TẠI trên thị trường VN hoặc quốc tế
            - demandScore: 1-10 (nhu cầu tuyển dụng)
            - Liệt kê công ty VN đang tuyển hoặc có vị trí tương tự
            - Mức lương thực tế tại VN
            - requiredSkills: kỹ năng cần có từ cả 2 ngành
            - growthOutlook: triển vọng tăng trưởng trong 5-10 năm tới

            ## VÍ DỤ NGÀNH HYBRID
            - Thiết kế + Lập trình → "UX Engineer", "Creative Developer", "Design Technologist"
            - Marketing + AI → "Growth Hacker", "AI Marketing Specialist"
            - Kế toán + IT → "FinTech Developer", "Data Analyst"
            - Tiếng Anh + IT → "Technical Writer", "Localization Engineer"
            - Du lịch + Marketing → "Travel Content Creator", "Destination Marketer"

            ## FORMAT: JSON hợp lệ
            {
              "major1Name": "...", "major2Name": "...",
              "hybridCareers": [
                {
                  "careerTitle": "...", "description": "Mô tả 2-3 câu...",
                  "demandScore": <1-10>, "salaryRange": "15 - 30 triệu",
                  "requiredSkills": ["Skill1", "Skill2", ...],
                  "companies": ["FPT", "VNG", ...],
                  "growthOutlook": "Triển vọng..."
                }
              ],
              "aiSummary": "Tóm tắt 2-3 câu về tiềm năng hybrid"
            }
            Viết bằng tiếng Việt.
            """;

    public HybridCareerResponse getHybridCareer(Long submissionId, Long majorId1, Long majorId2) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        Major major1 = majorRepo.findById(majorId1)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId1));
        Major major2 = majorRepo.findById(majorId2)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy ngành ID: " + majorId2));

        String systemPrompt = HYBRID_CAREER_SYSTEM_PROMPT
                .replace("{major1}", major1.getName())
                .replace("{major2}", major2.getName());

        String userPrompt = buildStudentDataPrompt(submission);
        userPrompt += "\n\nNgành 1: " + major1.getName();
        userPrompt += "\nNgành 2: " + major2.getName();

        long startTime = System.currentTimeMillis();
        String aiResponse = geminiAiService.generate(systemPrompt, userPrompt);
        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Hybrid Career AI xử lý xong trong {}ms", processingTime);

        return parseHybridResponse(aiResponse, major1.getName(), major2.getName());
    }

    private HybridCareerResponse parseHybridResponse(String aiResponse, String m1, String m2) {
        try {
            JsonNode root = objectMapper.readTree(aiResponse);
            List<HybridCareerResponse.HybridCareer> careers = new ArrayList<>();
            JsonNode careersNode = root.path("hybridCareers");
            if (careersNode.isArray()) {
                for (JsonNode c : careersNode) {
                    careers.add(new HybridCareerResponse.HybridCareer(
                            c.path("careerTitle").asText(""),
                            c.path("description").asText(""),
                            c.path("demandScore").asInt(5),
                            c.path("salaryRange").asText(""),
                            parseJsonArrayNode(c.has("requiredSkills") ? c.get("requiredSkills").toString() : "[]"),
                            parseJsonArrayNode(c.has("companies") ? c.get("companies").toString() : "[]"),
                            c.path("growthOutlook").asText("")
                    ));
                }
            }
            return new HybridCareerResponse(
                    root.path("major1Name").asText(m1),
                    root.path("major2Name").asText(m2),
                    careers,
                    root.path("aiSummary").asText("")
            );
        } catch (Exception e) {
            log.error("Lỗi parse Hybrid Career response: {}", aiResponse, e);
            throw new RuntimeException("Lỗi xử lý Hybrid Career response", e);
        }
    }
}
