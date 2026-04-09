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
 * Service gọi Gemini API trực tiếp qua REST (không dùng Spring AI dependency).
 */
@Service
@Slf4j
public class GeminiAiService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.gemini.api-key:}")
    private String apiKey;

    @Value("${spring.ai.gemini.model:gemini-2.0-flash}")
    private String model;

    public GeminiAiService(ObjectMapper objectMapper) {
        this.restClient = RestClient.builder()
                .baseUrl("https://generativelanguage.googleapis.com")
                .build();
        this.objectMapper = objectMapper;
    }

    public String getModel() {
        return model;
    }

    /**
     * Gọi Gemini API với system prompt + user prompt.
     * Trả về text response (JSON string).
     */
    public String generate(String systemPrompt, String userPrompt) {
        // Detect feature type from system prompt for mock fallback
        String featureType = detectFeatureType(systemPrompt);

        if (apiKey == null || apiKey.isBlank() || apiKey.equals("your_key_here")) {
            log.warn("GEMINI_API_KEY chưa được cấu hình. Trả về mock response ({}).", featureType);
            return getMockResponse(featureType);
        }

        String uri = String.format("/v1beta/models/%s:generateContent?key=%s", model, apiKey);

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
            log.info("Gọi Gemini API: model={}, feature={}", model, featureType);
            long start = System.currentTimeMillis();

            String responseJson = restClient.post()
                    .uri(uri)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(requestBody)
                    .retrieve()
                    .body(String.class);

            long elapsed = System.currentTimeMillis() - start;
            log.info("Gemini API phản hồi trong {}ms", elapsed);

            return extractTextFromResponse(responseJson);

        } catch (org.springframework.web.client.HttpClientErrorException.TooManyRequests e) {
            log.warn("Gemini API rate limit (429). Fallback sang mock response ({}).", featureType);
            return getMockResponse(featureType);
        } catch (Exception e) {
            log.error("Lỗi gọi Gemini API: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể gọi AI. Vui lòng thử lại sau.", e);
        }
    }

    /**
     * Xác định loại feature từ system prompt.
     */
    private String detectFeatureType(String systemPrompt) {
        if (systemPrompt.contains("lộ trình") || systemPrompt.contains("Fresher")) return "roadmap";
        if (systemPrompt.contains("Future-Proof") || systemPrompt.contains("6 chỉ số")) return "scorecard";
        if (systemPrompt.contains("So sánh")) return "compare";
        return "recommendation";
    }

    /**
     * Extract text content từ Gemini API response.
     */
    private String extractTextFromResponse(String responseJson) {
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

    /**
     * Mock response theo từng feature type.
     */
    private String getMockResponse(String featureType) {
        return switch (featureType) {
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
                      "reason": "Dựa trên sở thích công nghệ và tư duy logic của em, ngành Phát triển Phần mềm là lựa chọn rất phù hợp! Em sẽ được học Java, C#, xây dựng ứng dụng web từ A-Z. Mức lương khởi điểm hấp dẫn 12-18 triệu, có thể tăng nhanh lên 25-40 triệu chỉ sau 2-3 năm kinh nghiệm.",
                      "careerPaths": ["Lập trình viên Java/C#", "Kỹ sư phần mềm", "Quản lý dự án CNTT"],
                      "salaryRange": "12 - 40 triệu",
                      "skillsToImprove": ["Tiếng Anh chuyên ngành", "Thuật toán"]
                    },
                    {
                      "majorId": 2,
                      "majorName": "THIẾT KẾ ĐỒ HỌA",
                      "fieldName": "Thiết kế & Sáng tạo",
                      "matchScore": 75,
                      "reason": "Em thể hiện sự quan tâm đến sáng tạo và thẩm mỹ. Thiết kế Đồ họa cho phép em phát huy khả năng sáng tạo qua Photoshop, Illustrator và các dự án thực tế. Đây là ngành luôn được doanh nghiệp tìm kiếm.",
                      "careerPaths": ["Graphic Designer", "UI/UX Designer", "Brand Designer"],
                      "salaryRange": "8 - 25 triệu",
                      "skillsToImprove": ["Kỹ năng vẽ tay", "Portfolio chuyên nghiệp"]
                    },
                    {
                      "majorId": 3,
                      "majorName": "DIGITAL MARKETING",
                      "fieldName": "Kinh doanh & Quản trị",
                      "matchScore": 68,
                      "reason": "Với khả năng giao tiếp và sự nhạy bén, Digital Marketing là ngành giúp em kết hợp sáng tạo nội dung với phân tích dữ liệu. Cơ hội việc làm rất rộng mở trong thời đại số.",
                      "careerPaths": ["Chuyên viên Marketing", "Content Creator", "SEO Specialist"],
                      "salaryRange": "8 - 20 triệu",
                      "skillsToImprove": ["Phân tích dữ liệu", "Viết content"]
                    }
                  ]
                }
                """;
    }

    private String getMockRoadmap() {
        return """
                {
                  "overview": "Ngành này có triển vọng rất tốt tại Việt Nam, nhu cầu nhân lực tăng 15-20% mỗi năm. Sinh viên tốt nghiệp thường có việc làm ngay từ năm cuối với mức lương cạnh tranh.",
                  "phases": [
                    {
                      "title": "Giai đoạn 1: Fresher",
                      "period": "0-1 năm",
                      "description": "Bước chân vào ngành, học cách làm việc thực tế. Tập trung xây dựng nền tảng kiến thức và kỹ năng cơ bản, làm quen với môi trường doanh nghiệp.",
                      "skills": ["Kiến thức nền tảng", "Kỹ năng làm việc nhóm", "Giao tiếp cơ bản", "Sử dụng công cụ chuyên ngành"],
                      "salaryRange": "8-12 triệu",
                      "positions": ["Thực tập sinh", "Fresher", "Trợ lý"]
                    },
                    {
                      "title": "Giai đoạn 2: Junior",
                      "period": "1-3 năm",
                      "description": "Bắt đầu nhận dự án độc lập, phát triển kỹ năng chuyên sâu. Đây là giai đoạn tăng trưởng nhanh nhất về năng lực và thu nhập.",
                      "skills": ["Kỹ năng chuyên sâu", "Giải quyết vấn đề", "Quản lý thời gian", "Tiếng Anh chuyên ngành"],
                      "salaryRange": "12-20 triệu",
                      "positions": ["Junior Specialist", "Chuyên viên", "Nhân viên chính thức"]
                    },
                    {
                      "title": "Giai đoạn 3: Mid-level",
                      "period": "3-5 năm",
                      "description": "Trở thành chuyên gia trong lĩnh vực, có thể mentor cho junior. Bắt đầu tham gia quản lý dự án nhỏ và ra quyết định chuyên môn.",
                      "skills": ["Tư duy chiến lược", "Kỹ năng leadership", "Mentoring", "Quản lý dự án"],
                      "salaryRange": "20-35 triệu",
                      "positions": ["Senior Specialist", "Team Lead", "Quản lý nhóm"]
                    },
                    {
                      "title": "Giai đoạn 4: Senior / Manager",
                      "period": "5+ năm",
                      "description": "Vị trí cấp cao, định hướng chiến lược và phát triển đội ngũ. Có thể chuyển hướng sang quản lý hoặc tư vấn chuyên gia.",
                      "skills": ["Quản lý đội ngũ", "Tư duy business", "Networking", "Đào tạo nhân sự"],
                      "salaryRange": "35-60+ triệu",
                      "positions": ["Manager", "Director", "Chuyên gia tư vấn", "Freelancer cao cấp"]
                    }
                  ],
                  "certifications": [
                    "Chứng chỉ chuyên ngành quốc tế (tùy lĩnh vực)",
                    "IELTS 6.0+ hoặc TOEIC 650+",
                    "Chứng chỉ quản lý dự án (PMP hoặc tương đương)",
                    "Google Digital Garage / HubSpot Academy",
                    "Chứng chỉ kỹ năng mềm (Leadership, Communication)"
                  ],
                  "careerBranches": [
                    {
                      "name": "Chuyên gia kỹ thuật",
                      "description": "Tập trung phát triển chuyên môn sâu, trở thành expert trong lĩnh vực cụ thể. Phù hợp với người thích nghiên cứu và giải quyết bài toán khó.",
                      "tools": ["Công cụ chuyên ngành", "AI/Automation tools", "Data Analytics"],
                      "demandLevel": "Rất cao"
                    },
                    {
                      "name": "Quản lý & Điều phối",
                      "description": "Chuyển hướng sang quản lý dự án và đội ngũ. Kết hợp kiến thức chuyên môn với kỹ năng leadership.",
                      "tools": ["Jira/Trello", "MS Project", "Slack/Teams"],
                      "demandLevel": "Cao"
                    },
                    {
                      "name": "Tư vấn & Đào tạo",
                      "description": "Chia sẻ kinh nghiệm cho thế hệ tiếp theo. Làm việc với nhiều doanh nghiệp, tự do và thu nhập cao.",
                      "tools": ["Presentation tools", "LMS platforms", "Social Media"],
                      "demandLevel": "Cao"
                    },
                    {
                      "name": "Khởi nghiệp & Freelance",
                      "description": "Tự xây dựng sản phẩm hoặc cung cấp dịch vụ cho khách hàng quốc tế. Rủi ro cao nhưng tiềm năng thu nhập không giới hạn.",
                      "tools": ["Upwork/Fiverr", "No-code tools", "Business Canvas"],
                      "demandLevel": "Trung bình"
                    }
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
                    {
                      "name": "Phù hợp cá nhân",
                      "icon": "🎯",
                      "score": 82,
                      "label": "Tốt",
                      "explanation": "Sở thích và tính cách của em khá phù hợp với ngành này. Khả năng sáng tạo và tư duy logic là lợi thế lớn."
                    },
                    {
                      "name": "Kháng AI Automation",
                      "icon": "🛡️",
                      "score": 65,
                      "label": "Khá",
                      "explanation": "Một số công việc cơ bản có thể bị AI thay thế, nhưng kỹ năng sáng tạo và tư duy phức tạp vẫn cần con người."
                    },
                    {
                      "name": "Tác động xã hội",
                      "icon": "🌍",
                      "score": 70,
                      "label": "Tốt",
                      "explanation": "Ngành này đóng góp tích cực cho xã hội thông qua việc số hóa và cải thiện trải nghiệm người dùng."
                    },
                    {
                      "name": "Thu nhập & Tăng trưởng",
                      "icon": "💰",
                      "score": 85,
                      "label": "Xuất sắc",
                      "explanation": "Mức lương khởi điểm tốt (10-15 triệu) và tốc độ tăng trưởng nhanh, có thể gấp 3-4 lần sau 5 năm."
                    },
                    {
                      "name": "Linh hoạt chuyển ngành",
                      "icon": "🔄",
                      "score": 78,
                      "label": "Tốt",
                      "explanation": "Kỹ năng từ ngành này dễ dàng áp dụng sang nhiều lĩnh vực khác như fintech, edtech, e-commerce."
                    },
                    {
                      "name": "Độ hot VN 2026-2035",
                      "icon": "🔥",
                      "score": 88,
                      "label": "Xuất sắc",
                      "explanation": "Nhu cầu tuyển dụng rất cao, tăng 15-20%/năm. Việt Nam đang là điểm nóng về nhân lực công nghệ trong khu vực."
                    }
                  ],
                  "aiInsight": "Nhìn chung, ngành này là lựa chọn rất tốt cho em trong bối cảnh thị trường Việt Nam 2026-2035. Điểm mạnh nhất nằm ở thu nhập tiềm năng và độ hot, trong khi khả năng kháng AI ở mức khá - em nên tập trung phát triển kỹ năng sáng tạo và tư duy phức tạp để không bị thay thế. Với profile hiện tại, em có nền tảng tốt để phát triển trong ngành này."
                }
                """;
    }

    private String getMockCompare() {
        return """
                {
                  "major1": {
                    "matchScore": 85,
                    "reason": "Ngành này rất phù hợp với sở thích công nghệ và tư duy logic của em. Em sẽ được làm việc với các dự án thực tế ngay từ năm đầu.",
                    "careerPaths": ["Lập trình viên", "Kỹ sư phần mềm", "Tech Lead"],
                    "keySubjects": ["Lập trình", "Cơ sở dữ liệu", "Phát triển web"],
                    "skillsToImprove": ["Tiếng Anh", "Thuật toán"],
                    "studyDuration": "2.5 năm",
                    "admissionRequirements": "Tốt nghiệp THPT"
                  },
                  "major2": {
                    "matchScore": 72,
                    "reason": "Ngành phù hợp với sự sáng tạo của em, nhưng đòi hỏi kỹ năng thẩm mỹ cao. Em cần xây dựng portfolio mạnh để cạnh tranh.",
                    "careerPaths": ["Designer", "Art Director", "UI/UX"],
                    "keySubjects": ["Thiết kế đồ họa", "Typography", "Branding"],
                    "skillsToImprove": ["Kỹ năng vẽ", "Phần mềm Adobe"],
                    "studyDuration": "2.5 năm",
                    "admissionRequirements": "Tốt nghiệp THPT"
                  },
                  "conclusion": "Dựa trên profile của em, ngành đầu tiên phù hợp hơn vì em có tư duy logic mạnh và thích công nghệ. Mức lương và cơ hội việc làm cũng tốt hơn trong 5 năm tới. Tuy nhiên, nếu em thực sự đam mê sáng tạo nghệ thuật, ngành thứ hai cũng là lựa chọn tốt.",
                  "conclusionMajorId": 1
                }
                """;
    }
}
