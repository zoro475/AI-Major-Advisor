package com.example.prj.init;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.example.prj.entity.Field;
import com.example.prj.entity.Major;
import com.example.prj.entity.MajorSubject;
import com.example.prj.entity.MajorSubjectId;
import com.example.prj.entity.Subject;
import com.example.prj.repository.FieldRepository;
import com.example.prj.repository.MajorRepository;
import com.example.prj.repository.MajorSubjectRepository;
import com.example.prj.repository.SubjectRepository;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final MajorRepository majorRepository;
    private final FieldRepository fieldRepository;
    private final SubjectRepository subjectRepository;
    private final MajorSubjectRepository majorSubjectRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void run(String... args) throws Exception {
        if (majorRepository.count() > 0) {
            log.info("✅ Dữ liệu ngành học đã tồn tại. Bỏ qua import.");
            return;
        }

        log.info("🚀 Bắt đầu import dữ liệu từ dulieumau.json...");

        try {
            Resource resource = new ClassPathResource("data/dulieumau.json");
            List<Map<String, Object>> majorsData = objectMapper.readValue(
                    resource.getInputStream(),
                    new TypeReference<List<Map<String, Object>>>() {}
            );

            int importedCount = 0;
            for (Map<String, Object> item : majorsData) {
                importSingleMajor(item);
                importedCount++;
            }

            log.info("🎉 Import hoàn tất! Tổng số ngành đã import: {}", importedCount);

        } catch (Exception e) {
            log.error("❌ Lỗi khi import dữ liệu từ JSON", e);
        }
    }

    private void importSingleMajor(Map<String, Object> item) {
        String majorName = (String) item.get("name");

        // 1. Xác định Field
        Field tempField = determineField(majorName);
        Field savedField = fieldRepository.findByName(tempField.getName())
                .orElseGet(() -> fieldRepository.save(tempField));

        // 2. Kiểm tra Major đã tồn tại chưa
        Optional<Major> existingMajor = majorRepository.findByName(majorName);
        if (existingMajor.isPresent()) {
            log.info("Ngành '{}' đã tồn tại, bỏ qua.", majorName);
            return;
        }

        // 3. Tạo Major
        Major major = Major.builder()
                .field(savedField)
                .name(majorName)
                .url((String) item.get("url"))
                .imageUrl((String) item.get("imageUrl"))
                .target((String) item.get("target"))
                .careerOpportunities((String) item.get("careerOpportunities"))
                .hotTrendScore(6)
                .aiResistanceScore(calculateAiResistance(majorName))
                .salaryRange(estimateSalaryRange(majorName))
                .build();

        major = majorRepository.save(major);

        // 4. Xử lý Subjects
        @SuppressWarnings("unchecked")
        List<String> subjectsBlocks = (List<String>) item.get("subjects");
        if (subjectsBlocks != null) {
            processSubjects(major, subjectsBlocks);
        }

        log.info("✅ Đã import ngành: {}", majorName);
    }

    // ====================== Helper Methods ======================

    private Field determineField(String majorName) {
        String fieldName = "Khác";

        if (majorName.contains("THIẾT KẾ") || majorName.contains("ĐỒ HỌA") ||
                majorName.contains("TRUYỀN THÔNG") || majorName.contains("MARKETING") ||
                majorName.contains("DIGITAL")) {
            fieldName = "Thiết kế & Marketing";
        } else if (majorName.contains("CÔNG NGHỆ") || majorName.contains("LẬP TRÌNH") ||
                majorName.contains("PHẦN MỀM") || majorName.contains("AI") ||
                majorName.contains("DỮ LIỆU") || majorName.contains("WEB") ||
                majorName.contains("MOBILE") || majorName.contains("GAME")) {
            fieldName = "Công nghệ Thông tin";
        } else if (majorName.contains("TIẾNG")) {
            fieldName = "Ngoại ngữ";
        } else if (majorName.contains("QUẢN TRỊ") || majorName.contains("KẾ TOÁN") ||
                majorName.contains("LOGISTICS") || majorName.contains("SALES") ||
                majorName.contains("DU LỊCH") || majorName.contains("KHÁCH SẠN")) {
            fieldName = "Kinh doanh & Dịch vụ";
        } else if (majorName.contains("DƯỢC")) {
            fieldName = "Y tế & Dược";
        }

        return Field.builder().name(fieldName).build();
    }

    private int calculateAiResistance(String majorName) {
        if (majorName.contains("AI") || majorName.contains("TRÍ TUỆ NHÂN TẠO")) return 9;
        if (majorName.contains("LẬP TRÌNH") || majorName.contains("PHÁT TRIỂN")) return 8;
        if (majorName.contains("TIẾNG")) return 7;
        if (majorName.contains("KẾ TOÁN") || majorName.contains("MARKETING")) return 4;
        return 6;
    }

    private String estimateSalaryRange(String majorName) {
        if (majorName.contains("AI") || majorName.contains("LẬP TRÌNH")) return "15 - 35 triệu";
        if (majorName.contains("TIẾNG")) return "10 - 25 triệu";
        return "10 - 25 triệu";
    }

    private void processSubjects(Major major, List<String> subjectsBlocks) {
        for (String block : subjectsBlocks) {
            if (block == null || block.trim().isEmpty()) continue;

            String[] semesterParts = block.split("Học kỳ");
            for (String part : semesterParts) {
                if (part.trim().isEmpty()) continue;

                Integer semester = extractSemester(part);
                String[] subjectNames = part.split("\\|");

                for (String sub : subjectNames) {
                    String subjectName = sub.trim();
                    if (subjectName.isEmpty() ||
                            subjectName.contains("Hà Nội") ||
                            subjectName.contains("TP HCM") ||
                            subjectName.contains("các cơ sở")) {
                        continue;
                    }

                    Subject subject = subjectRepository.findByName(subjectName)
                            .orElseGet(() -> subjectRepository.save(
                                    Subject.builder()
                                            .name(subjectName)
                                            .semester(semester)
                                            .build()
                            ));

                    MajorSubject majorSubject = new MajorSubject(
                            new MajorSubjectId(major.getId(), subject.getId()),
                            major,
                            subject
                    );
                    majorSubjectRepository.save(majorSubject);
                }
            }
        }
    }

    private Integer extractSemester(String text) {
        if (text.contains("1")) return 1;
        if (text.contains("2")) return 2;
        if (text.contains("3")) return 3;
        if (text.contains("4")) return 4;
        if (text.contains("5")) return 5;
        if (text.contains("6")) return 6;
        return null;
    }
}