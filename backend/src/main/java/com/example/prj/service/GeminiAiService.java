package com.example.prj.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * AI Service hỗ trợ cả Gemini API và Ollama local.
 */
@Service
@Slf4j
public class GeminiAiService {

    private final RestClient geminiClient;
    private final RestClient ollamaClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${spring.ai.gemini.model:gemini-2.0-flash}")
    private String geminiModel;

    @Value("${spring.ai.provider:gemini}")
    private String provider;

    @Value("${spring.ai.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${spring.ai.ollama.model:gemma3:4b}")
    private String ollamaModel;

    public GeminiAiService(ObjectMapper objectMapper) {
        this.geminiClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.ollamaClient = RestClient.builder()
                .baseUrl("http://localhost:11434")
                .build();
        this.objectMapper = objectMapper;
    }

    public String getModel() {
        return "ollama".equalsIgnoreCase(provider) ? ollamaModel : geminiModel;
    }

    /**
     * Gọi AI với system prompt + user prompt.
     * Tự động chọn provider (Gemini hoặc Ollama).
     */
    public String generate(String systemPrompt, String userPrompt) {
        String featureType = detectFeatureType(systemPrompt);

        if ("ollama".equalsIgnoreCase(provider)) {
            return generateWithOllama(systemPrompt, userPrompt, featureType);
        } else {
            return generateWithGemini(systemPrompt, userPrompt, featureType);
        }
    }

    // ==================== Ollama ====================

    private String generateWithOllama(String systemPrompt, String userPrompt, String featureType) {
        Map<String, Object> requestBody = Map.of(
                "model", ollamaModel,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt + "\n\nTrả về JSON hợp lệ, bắt đầu bằng { và kết thúc bằng }. Không viết gì trước hoặc sau JSON.")
                ),
                "stream", false,
                "options", Map.of(
                        "temperature", 0.7,
                        "num_predict", 8192
                )
        );

        try {
            log.info("Gọi Ollama API: model={}, feature={}", ollamaModel, featureType);
            long start = System.currentTimeMillis();

            String responseJson = ollamaClient.post()
                    .uri("/api/chat")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("Ollama phản hồi trong {}ms", elapsed);

            String content = extractOllamaResponse(responseJson);
            return extractJsonFromText(content);

        } catch (Exception e) {
            log.error("Lỗi gọi Ollama: {}. Fallback sang mock.", e.getMessage());
            return getMockResponse(featureType);
        }
    }

    private String extractOllamaResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            String content = root.path("message").path("content").asText();
            if (content != null && !content.isBlank()) {
                return content;
            }
            log.error("Ollama response trống: {}", responseJson);
            throw new RuntimeException("Ollama response trống");
        } catch (Exception e) {
            log.error("Lỗi parse Ollama response", e);
            throw new RuntimeException("Lỗi xử lý phản hồi Ollama", e);
        }
    }

    /**
     * Extract JSON object từ text response (bỏ markdown, text thừa).
     */
    private String extractJsonFromText(String text) {
        if (text == null || text.isBlank()) return "{}";

        // Tìm JSON block trong markdown ```json ... ```
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("```(?:json)?\\s*\\n?(\\{[\\s\\S]*?\\})\\s*```")
                .matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Tìm JSON object đầu tiên { ... }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        log.warn("Không tìm thấy JSON trong Ollama response: {}", text.substring(0, Math.min(200, text.length())));
        return text;
    }

    // ==================== Gemini ====================

    private String generateWithGemini(String systemPrompt, String userPrompt, String featureType) {
        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_key_here")) {
            log.warn("GEMINI_API_KEY chưa được cấu hình. Trả về mock response ({}).", featureType);
            return getMockResponse(featureType);
        }

        String uri = String.format("/v1beta/models/%s:generateContent?key=%s", geminiModel, apiKey);

        Map<String, Object> requestBody = Map.of(
                "system_instruction", Map.of(
                        "parts", List.of(Map.of("text", systemPrompt))
                ),
                "contents", List.of(
                        Map.of("parts", List.of(Map.of("text", userPrompt)))
                ),
                "generationConfig", Map.of(
                        "temperature", 0.7,
                        "maxOutputTokens", 4096,
                        "responseMimeType", "application/json"
                )
        );

        try {
            log.info("Gọi Gemini API: model={}, feature={}", geminiModel, featureType);
            long start = System.currentTimeMillis();

            String responseJson = geminiClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("Gemini API phản hồi trong {}ms", elapsed);

            return extractGeminiResponse(responseJson);

        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini API rate limit (429). Fallback sang mock response ({}).", featureType);
            return getMockResponse(featureType);
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gọi AI. Vui lòng thử lại sau.", e);
        }
    }

    private String extractGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    return parts.get(0).path("text").asText();
                }
            }
            log.error("Không thể extract text từ Gemini response: {}", responseJson);
            throw new RuntimeException("AI response format không hợp lệ");
        } catch (Exception e) {
            log.error("Lỗi parse Gemini response", e);
            throw new RuntimeException("Lỗi xử lý phản hồi AI", e);
        }
    }

    // ==================== Utils ====================

    private String detectFeatureType(String systemPrompt) {
        if (systemPrompt.contains("Cỗ máy thời gian")) return "timemachine";
        if (systemPrompt.contains("What-If") || systemPrompt.contains("mô phỏng")) return "whatif";
        if (systemPrompt.contains("HYBRID")) return "hybrid";
        if (systemPrompt.contains("lộ trình") || systemPrompt.contains("Fresher")) return "roadmap";
        if (systemPrompt.contains("Future-Proof") || systemPrompt.contains("6 chỉ số")) return "scorecard";
        if (systemPrompt.contains("So sánh")) return "compare";
        return "recommendation";
    }

    private String getMockResponse(String featureType) {
        return switch (featureType) {
            case "timemachine" -> getMockTimeMachine();
            case "whatif" -> getMockWhatIf();
            case "hybrid" -> getMockHybrid();
            case "roadmap" -> getMockRoadmap();
            case "scorecard" -> getMockScorecard();
            case "compare" -> getMockCompare();
            default -> getMockRecommendation();
        };
    }

    private String getMockRecommendation() {
        return """
                {
                  "summary": "Học sinh có xu hướng yêu thích công nghệ, sáng tạo và mong muốn có thu nhập cao sau khi ra trường.",
                  "recommendations": [
                    {
                      "majorId": 1,
                      "majorName": "PHÁT TRIỂN PHẦN MỀM",
                      "fieldName": "Công nghệ Thông tin & Kỹ thuật",
                      "matchScore": 88,
                      "reason": "Dựa trên sở thích công nghệ và tư duy logic của em, ngành Phát triển Phần mềm là lựa chọn rất phù hợp!",
                      "careerPaths": ["Lập trình viên Java/C#", "Kỹ sư phần mềm", "Quản lý dự án CNTT"],
                      "salaryRange": "12 - 40 triệu",
                      "skillsToImprove": ["Tiếng Anh chuyên ngành", "Thuật toán"]
                    },
                    {
                      "majorId": 2,
                      "majorName": "THIẾT KẾ ĐỒ HỌA",
                      "fieldName": "Thiết kế & Sáng tạo",
                      "matchScore": 75,
                      "reason": "Em thể hiện sự quan tâm đến sáng tạo và thẩm mỹ.",
                      "careerPaths": ["Graphic Designer", "UI/UX Designer", "Brand Designer"],
                      "salaryRange": "8 - 25 triệu",
                      "skillsToImprove": ["Kỹ năng vẽ tay", "Portfolio chuyên nghiệp"]
                    }
                  ]
                }
                """;
    }

    private String getMockRoadmap() {
        return """
                {
                  "overview": "Ngành này có triển vọng rất tốt tại Việt Nam.",
                  "phases": [
                    {"title": "Fresher", "period": "0-1 năm", "description": "Bước chân vào ngành.", "skills": ["Kiến thức nền tảng", "Làm việc nhóm"], "salaryRange": "8-12 triệu", "positions": ["Thực tập sinh", "Fresher"]},
                    {"title": "Junior", "period": "1-3 năm", "description": "Phát triển kỹ năng chuyên sâu.", "skills": ["Kỹ năng chuyên sâu", "Tiếng Anh"], "salaryRange": "12-20 triệu", "positions": ["Junior", "Chuyên viên"]},
                    {"title": "Mid-level", "period": "3-5 năm", "description": "Trở thành chuyên gia.", "skills": ["Leadership", "Quản lý dự án"], "salaryRange": "20-35 triệu", "positions": ["Senior", "Team Lead"]},
                    {"title": "Senior", "period": "5+ năm", "description": "Vị trí cấp cao.", "skills": ["Quản lý đội ngũ", "Tư duy business"], "salaryRange": "35-60+ triệu", "positions": ["Manager", "Director"]}
                  ],
                  "certifications": ["Chứng chỉ chuyên ngành quốc tế", "IELTS 6.0+", "PMP"],
                  "careerBranches": [
                    {"name": "Chuyên gia kỹ thuật", "description": "Phát triển chuyên môn sâu.", "tools": ["Công cụ chuyên ngành", "AI tools"], "demandLevel": "Rất cao"},
                    {"name": "Quản lý", "description": "Chuyển hướng quản lý.", "tools": ["Jira", "MS Project"], "demandLevel": "Cao"},
                    {"name": "Khởi nghiệp", "description": "Tự xây dựng sản phẩm.", "tools": ["No-code tools", "Business Canvas"], "demandLevel": "Trung bình"}
                  ]
                }
                """;
    }

    private String getMockScorecard() {
        return """
                {
                  "overallScore": 76,
                  "overallVerdict": "Tốt",
                  "metrics": [
                    {"name": "Phù hợp cá nhân", "icon": "🎯", "score": 82, "label": "Tốt", "explanation": "Sở thích và tính cách phù hợp."},
                    {"name": "Kháng AI Automation", "icon": "🛡️", "score": 65, "label": "Khá", "explanation": "Kỹ năng sáng tạo vẫn cần con người."},
                    {"name": "Tác động xã hội", "icon": "🌍", "score": 70, "label": "Tốt", "explanation": "Đóng góp tích cực cho xã hội."},
                    {"name": "Thu nhập & Tăng trưởng", "icon": "💰", "score": 85, "label": "Xuất sắc", "explanation": "Lương tốt, tăng trưởng nhanh."},
                    {"name": "Linh hoạt chuyển ngành", "icon": "🔄", "score": 78, "label": "Tốt", "explanation": "Dễ áp dụng sang lĩnh vực khác."},
                    {"name": "Độ hot VN 2026-2035", "icon": "🔥", "score": 88, "label": "Xuất sắc", "explanation": "Nhu cầu tuyển dụng rất cao."}
                  ],
                  "aiInsight": "Ngành này là lựa chọn rất tốt trong bối cảnh Việt Nam 2026-2035."
                }
                """;
    }

    private String getMockCompare() {
        return """
                {
                  "major1": {"matchScore": 85, "reason": "Rất phù hợp.", "careerPaths": ["Dev", "Tech Lead"], "keySubjects": ["Lập trình", "DB"], "skillsToImprove": ["English"], "studyDuration": "2.5 năm", "admissionRequirements": "THPT"},
                  "major2": {"matchScore": 72, "reason": "Phù hợp với sáng tạo.", "careerPaths": ["Designer", "UI/UX"], "keySubjects": ["Thiết kế", "Branding"], "skillsToImprove": ["Kỹ năng vẽ"], "studyDuration": "2.5 năm", "admissionRequirements": "THPT"},
                  "conclusion": "Ngành đầu tiên phù hợp hơn.",
                  "conclusionMajorId": 1
                }
                """;
    }

    private String getMockTimeMachine() {
        return """
                {
                  "majorName": "Ngành học",
                  "studentProfile": "Em là học sinh có sở thích công nghệ và sáng tạo.",
                  "snapshots": [
                    {
                      "yearsFromNow": 5, "title": "Fresher → Junior", "emoji": "🌱",
                      "dayInLife": {"morning": "7h sáng, em đến văn phòng ở quận 7, TP.HCM. Mở laptop, check email và bắt đầu code feature mới cho dự án.", "afternoon": "Chiều họp team review, thảo luận solution cho bug khó. Em được senior hướng dẫn cách optimize code.", "evening": "Tối em học thêm khóa React trên Udemy, vừa xem vừa thực hành.", "highlight": "Hôm nay em fix được bug mà cả team stuck 3 ngày!"},
                      "salaryRange": "12 - 18 triệu", "achievements": ["Hoàn thành dự án đầu tiên", "Đạt chứng chỉ chuyên ngành"],
                      "challenges": [{"name": "Thiếu kinh nghiệm", "description": "Code chưa clean, hay bị review lại.", "howToOvercome": "Đọc Clean Code, học từ code của senior."}],
                      "opportunities": ["Công ty đang mở rộng, cần người", "Freelance online kiếm thêm"],
                      "worstCase": {"scenario": "Em chỉ làm theo chỉ dẫn, không tự học thêm.", "consequences": "Sau 5 năm vẫn ở vị trí junior, lương không tăng.", "preventionTip": "Mỗi ngày dành 1h học công nghệ mới."}
                    },
                    {
                      "yearsFromNow": 10, "title": "Senior / Team Lead", "emoji": "🚀",
                      "dayInLife": {"morning": "8h sáng, em review PR của team, lên plan cho sprint mới.", "afternoon": "Chiều em họp với khách hàng Nhật, present solution bằng tiếng Anh.", "evening": "Tối đi mentor cho nhóm sinh viên FPT Polytechnic.", "highlight": "Em vừa được promote lên Team Lead!"},
                      "salaryRange": "25 - 40 triệu", "achievements": ["Lead team 8 người", "Xây dựng sản phẩm có 100K users"],
                      "challenges": [{"name": "Quản lý con người", "description": "Khó balance giữa code và manage.", "howToOvercome": "Học soft skills, delegate công việc."}],
                      "opportunities": ["Remote cho công ty nước ngoài", "Co-founder startup"],
                      "worstCase": {"scenario": "Em chỉ giỏi code nhưng không phát triển leadership.", "consequences": "Bị đồng nghiệp trẻ hơn vượt qua về tốc độ.", "preventionTip": "Phát triển cả kỹ năng mềm và tư duy kinh doanh."}
                    },
                    {
                      "yearsFromNow": 15, "title": "Manager / CTO", "emoji": "👑",
                      "dayInLife": {"morning": "9h sáng, em họp với board of directors về chiến lược công nghệ.", "afternoon": "Chiều interview ứng viên senior, review architecture cho sản phẩm mới.", "evening": "Tối em viết blog chia sẻ kinh nghiệm trên LinkedIn.", "highlight": "Sản phẩm em lead vừa đạt 1 triệu users!"},
                      "salaryRange": "50 - 80+ triệu", "achievements": ["CTO công ty 200 nhân sự", "Speaker tại hội thảo quốc tế"],
                      "challenges": [{"name": "Giữ vững vị thế", "description": "Công nghệ thay đổi nhanh, cần liên tục cập nhật.", "howToOvercome": "Tham gia cộng đồng, đi conference, build network."}],
                      "opportunities": ["Tự mở công ty riêng", "Đầu tư angel cho startup"],
                      "worstCase": {"scenario": "Em ngừng học hỏi, dựa vào kinh nghiệm cũ.", "consequences": "Bị thị trường đào thải, khó tìm việc mới.", "preventionTip": "Never stop learning. Mỗi năm học ít nhất 1 công nghệ mới."}
                    }
                  ],
                  "overallMessage": "Hành trình 15 năm sẽ thật tuyệt vời nếu em không ngừng học hỏi và phát triển! 🌟"
                }
                """;
    }

    private String getMockWhatIf() {
        return """
                {
                  "originalProfile": "Em là học sinh thích sáng tạo, thiên về nghệ thuật.",
                  "whatIfProfile": "Với thêm kỹ năng lập trình, em trở thành ứng viên đa năng sáng tạo-kỹ thuật.",
                  "changes": [
                    {"majorId": 1, "majorName": "THIẾT KẾ ĐỒ HỌA", "originalScore": 85, "newScore": 90, "scoreDelta": 5, "changeReason": "Kỹ năng lập trình bổ trợ cho thiết kế UI/UX, tăng giá trị nghề nghiệp."},
                    {"majorId": 2, "majorName": "ỨNG DỤNG PHẦN MỀM", "originalScore": 55, "newScore": 75, "scoreDelta": 20, "changeReason": "Thêm kỹ năng lập trình giúp em phù hợp hơn với ngành phần mềm."},
                    {"majorId": 3, "majorName": "MARKETING & SALES", "originalScore": 70, "newScore": 65, "scoreDelta": -5, "changeReason": "Chuyển hướng sang kỹ thuật làm giảm nhẹ sự phù hợp với marketing truyền thống."}
                  ],
                  "aiAnalysis": "Việc thêm kỹ năng lập trình mở ra cánh cửa ngành CNTT (+20 điểm Ứng dụng phần mềm), đồng thời tăng giá trị cho Thiết kế đồ họa. Đây là hướng đi rất tiềm năng nếu em muốn trở thành Creative Developer."
                }
                """;
    }

    private String getMockHybrid() {
        return """
                {
                  "major1Name": "THIẾT KẾ ĐỒ HỌA",
                  "major2Name": "ỨNG DỤNG PHẦN MỀM",
                  "hybridCareers": [
                    {
                      "careerTitle": "UX/UI Engineer",
                      "description": "Kết hợp kỹ năng thiết kế và lập trình để tạo giao diện người dùng đẹp mắt và chức năng. Vừa code vừa design.",
                      "demandScore": 9,
                      "salaryRange": "18 - 40 triệu",
                      "requiredSkills": ["Figma", "React", "CSS", "User Research", "JavaScript"],
                      "companies": ["FPT Software", "VNG", "Shopee Vietnam", "Tiki"],
                      "growthOutlook": "Nhu cầu rất cao trong 5-10 năm tới khi mọi sản phẩm đều cần UX tốt."
                    },
                    {
                      "careerTitle": "Creative Developer",
                      "description": "Lập trình viên chuyên tạo trải nghiệm web sáng tạo với animation, 3D, interactive design.",
                      "demandScore": 7,
                      "salaryRange": "20 - 45 triệu",
                      "requiredSkills": ["Three.js", "WebGL", "GSAP", "React", "After Effects"],
                      "companies": ["Agency Ogilvy VN", "Dentsu VN", "Freelance quốc tế"],
                      "growthOutlook": "Thị trường ngách nhưng thu nhập cao, đặc biệt freelance quốc tế."
                    },
                    {
                      "careerTitle": "Design Technologist",
                      "description": "Cầu nối giữa team Design và team Dev, prototype nhanh và đảm bảo design feasibility.",
                      "demandScore": 6,
                      "salaryRange": "15 - 35 triệu",
                      "requiredSkills": ["Figma", "HTML/CSS", "React", "Design Systems"],
                      "companies": ["Google VN", "Microsoft VN", "Grab"],
                      "growthOutlook": "Vai trò đang được các công ty lớn tích cực tuyển dụng."
                    }
                  ],
                  "aiSummary": "Kết hợp Thiết kế đồ họa và Ứng dụng phần mềm mở ra nhiều nghề hybrid rất hot như UX Engineer, Creative Developer. Đây là combo cực kỳ giá trị trên thị trường hiện tại! 🔥"
                }
                """;
    }
}
