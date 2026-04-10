package com.example.prj.service;

import com.example.prj.entity.*;
import com.example.prj.repository.RecommendationResultRepository;
import com.example.prj.repository.SurveySubmissionRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportService {

    private final SurveySubmissionRepository submissionRepo;
    private final RecommendationResultRepository resultRepo;

    public byte[] generatePdf(Long submissionId) {
        SurveySubmission submission = submissionRepo.findByIdWithAnswers(submissionId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bài khảo sát ID: " + submissionId));

        RecommendationResult result = resultRepo.findBySubmissionIdWithItems(submissionId)
                .orElseThrow(() -> new RuntimeException("Chưa có kết quả phân tích cho submission: " + submissionId));

        String shareUrl = "http://localhost:5173/share/" + submissionId;
        String qrBase64 = generateQRCodeBase64(shareUrl);

        String html = buildHtml(submission, result, qrBase64);
        return renderPdf(html);
    }

    private String buildHtml(SurveySubmission submission, RecommendationResult result, String qrBase64) {
        StringBuilder sb = new StringBuilder();
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        sb.append("""
                <!DOCTYPE html>
                <html>
                <head>
                <meta charset="UTF-8"/>
                <style>
                @page { size: A4; margin: 30px 40px; }
                body { font-family: 'Arial', sans-serif; color: #1a1a2e; font-size: 12px; line-height: 1.6; }
                .cover { text-align: center; padding: 120px 0 60px; page-break-after: always; }
                .cover h1 { font-size: 28px; color: #8b5cf6; margin-bottom: 8px; }
                .cover h2 { font-size: 20px; color: #374151; font-weight: 400; }
                .cover .student-name { font-size: 32px; color: #06b6d4; font-weight: 700; margin: 40px 0 20px; }
                .cover .date { color: #9ca3af; font-size: 14px; margin-top: 40px; }
                .cover .logo { font-size: 48px; margin-bottom: 20px; }
                h2 { color: #8b5cf6; font-size: 16px; border-bottom: 2px solid #e5e7eb; padding-bottom: 6px; margin-top: 30px; }
                h3 { color: #374151; font-size: 13px; margin-top: 16px; }
                .section { margin-bottom: 20px; }
                .rec-card { background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 8px; padding: 14px; margin-bottom: 12px; }
                .rec-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; }
                .rec-name { font-weight: 700; font-size: 14px; color: #1e293b; }
                .score-badge { background: #8b5cf6; color: white; padding: 3px 12px; border-radius: 20px; font-weight: 700; font-size: 13px; }
                .rec-field { color: #64748b; font-size: 11px; margin-bottom: 6px; }
                .rec-reason { font-size: 11px; color: #475569; }
                .tag { display: inline-block; padding: 2px 8px; border-radius: 4px; font-size: 10px; margin: 2px; background: #e0e7ff; color: #4f46e5; }
                .tag-green { background: #d1fae5; color: #065f46; }
                .salary { color: #059669; font-weight: 600; }
                table { width: 100%; border-collapse: collapse; margin-top: 8px; }
                td, th { padding: 6px 10px; border: 1px solid #e5e7eb; font-size: 11px; text-align: left; }
                th { background: #f1f5f9; font-weight: 600; color: #374151; }
                .qr-section { text-align: center; margin-top: 40px; page-break-before: always; padding-top: 60px; }
                .qr-section img { width: 180px; height: 180px; }
                .qr-section p { color: #6b7280; font-size: 12px; margin-top: 12px; }
                .footer-text { text-align: center; color: #9ca3af; font-size: 10px; margin-top: 40px; }
                .summary-box { background: #eff6ff; border-left: 4px solid #3b82f6; padding: 12px 16px; border-radius: 4px; margin: 12px 0; }
                .profile-grid { display: flex; flex-wrap: wrap; gap: 8px; }
                .profile-item { background: #f1f5f9; padding: 6px 12px; border-radius: 6px; font-size: 11px; }
                </style>
                </head>
                <body>
                """);

        // Page 1: Cover
        sb.append(String.format("""
                <div class="cover">
                  <div class="logo">&#127891;</div>
                  <h1>H&#7890; S&#416; T&#431; V&#7844;N NGH&#7872; NGHI&#7878;P</h1>
                  <h2>AI Career Advisor - FPT Polytechnic</h2>
                  <div class="student-name">%s</div>
                  <div class="date">Ngay tao: %s</div>
                  <div class="date">Model: %s | Xu ly: %.1fs</div>
                </div>
                """, esc(submission.getStudentName()), now, esc(result.getModelUsed()),
                result.getProcessingTimeMs() / 1000.0));

        // Page 2: AI Summary + Profile
        sb.append("<h2>Tong quan AI</h2>");
        sb.append(String.format("<div class=\"summary-box\">%s</div>", esc(result.getAiSummary())));

        sb.append("<h2>Thong tin hoc sinh</h2>");
        sb.append("<table>");
        sb.append(String.format("<tr><th>Ho ten</th><td>%s</td></tr>", esc(submission.getStudentName())));
        if (submission.getStudentEmail() != null)
            sb.append(String.format("<tr><th>Email</th><td>%s</td></tr>", esc(submission.getStudentEmail())));
        if (submission.getFreeTextDescription() != null)
            sb.append(String.format("<tr><th>Mo ta ban than</th><td>%s</td></tr>", esc(submission.getFreeTextDescription())));
        sb.append("</table>");

        // Answers summary
        if (submission.getAnswers() != null && !submission.getAnswers().isEmpty()) {
            sb.append("<h3>Cau tra loi khao sat</h3><table><tr><th>Cau hoi</th><th>Tra loi</th></tr>");
            for (SurveyAnswer a : submission.getAnswers()) {
                sb.append(String.format("<tr><td>%s</td><td>%s</td></tr>",
                        a.getQuestion() != null ? esc(a.getQuestion().getQuestionText()) : "Cau " + a.getQuestion().getId(),
                        esc(a.getAnswerValue())));
            }
            sb.append("</table>");
        }

        // Page 3: Recommendations
        sb.append("<div style=\"page-break-before: always;\"></div>");
        sb.append("<h2>Nganh de xuat</h2>");

        List<RecommendationItem> items = result.getItems() != null ?
                result.getItems().stream().toList() : List.of();

        for (int i = 0; i < items.size(); i++) {
            RecommendationItem item = items.get(i);
            sb.append(String.format("""
                    <div class="rec-card">
                      <div class="rec-header">
                        <span class="rec-name">#%d %s</span>
                        <span class="score-badge">%d%%</span>
                      </div>
                      <div class="rec-field">%s | %s</div>
                      <div class="rec-reason">%s</div>
                    """,
                    i + 1, esc(item.getMajorName()), item.getMatchScore(),
                    esc(item.getFieldName()), item.getSalaryRange() != null ? esc(item.getSalaryRange()) : "N/A",
                    esc(item.getReason())));

            // Career paths
            if (item.getCareerPaths() != null) {
                sb.append("<div style=\"margin-top:6px\">");
                try {
                    var paths = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(item.getCareerPaths(), String[].class);
                    for (String p : paths) sb.append(String.format("<span class=\"tag\">%s</span>", esc(p)));
                } catch (Exception ignored) {}
                sb.append("</div>");
            }

            // Skills to improve
            if (item.getSkillsToImprove() != null) {
                sb.append("<div style=\"margin-top:4px\">");
                try {
                    var skills = new com.fasterxml.jackson.databind.ObjectMapper()
                            .readValue(item.getSkillsToImprove(), String[].class);
                    for (String s : skills) sb.append(String.format("<span class=\"tag tag-green\">%s</span>", esc(s)));
                } catch (Exception ignored) {}
                sb.append("</div>");
            }

            sb.append("</div>");
        }

        // QR Code page
        sb.append(String.format("""
                <div class="qr-section">
                  <h2>Xem ket qua Online</h2>
                  <p>Quet ma QR ben duoi de xem ket qua chi tiet tren trinh duyet</p>
                  <img src="data:image/png;base64,%s" alt="QR Code"/>
                  <p>Hoac truy cap: http://localhost:5173/share/%d</p>
                  <div class="footer-text">
                    <p>Powered by AI Career Advisor - FPT Polytechnic</p>
                    <p>Bao cao duoc tao tu dong boi he thong tu van huong nghiep AI</p>
                    <p>2026 FPT Polytechnic. Moi quyen duoc bao luu.</p>
                  </div>
                </div>
                """, qrBase64, submission.getId()));

        sb.append("</body></html>");
        return sb.toString();
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        } catch (Exception e) {
            log.error("Lỗi tạo PDF", e);
            throw new RuntimeException("Không thể tạo PDF", e);
        }
    }

    private String generateQRCodeBase64(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(text, BarcodeFormat.QR_CODE, 300, 300);
            BufferedImage image = MatrixToImageWriter.toBufferedImage(matrix);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "PNG", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (WriterException | IOException e) {
            log.error("Lỗi tạo QR code", e);
            return "";
        }
    }

    /** Escape HTML special characters for XHTML compatibility */
    private String esc(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
