import { useState, useEffect } from 'react';
import type { CompareResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './MajorComparison.css';

interface Props {
  submissionId: number;
  majorId1: number;
  majorId2: number;
  majorName1: string;
  majorName2: string;
  onClose: () => void;
}

export function MajorComparison({ submissionId, majorId1, majorId2, majorName1, majorName2, onClose }: Props) {
  const [result, setResult] = useState<CompareResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const compare = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await recommendationApi.compare(submissionId, majorId1, majorId2);
        setResult(data);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Lỗi so sánh');
      } finally {
        setLoading(false);
      }
    };
    compare();
  }, [submissionId, majorId1, majorId2]);

  if (loading) {
    return (
      <div className="compare-overlay">
        <div className="compare-modal">
          <div className="compare-loading">
            <div className="compare-spinner"></div>
            <h3>⚖️ AI đang so sánh...</h3>
            <p>Đang phân tích <strong>{majorName1}</strong> vs <strong>{majorName2}</strong></p>
          </div>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="compare-overlay">
        <div className="compare-modal">
          <div className="compare-error">
            <h3>❌ Lỗi so sánh</h3>
            <p>{error}</p>
            <button onClick={onClose}>Đóng</button>
          </div>
        </div>
      </div>
    );
  }

  if (!result) return null;

  const { major1, major2 } = result;
  const winner = result.conclusionMajorId;

  const renderScoreBar = (score: number, isWinner: boolean) => (
    <div className="score-bar-container">
      <div
        className={`score-bar ${isWinner ? 'winner' : ''}`}
        style={{ width: `${score}%` }}
      >
        <span>{score}%</span>
      </div>
    </div>
  );

  const renderStars = (score: number, max: number = 10) => {
    const filled = Math.round((score / max) * 5);
    return (
      <span className="star-rating">
        {'★'.repeat(filled)}{'☆'.repeat(5 - filled)}
        <span className="star-value">{score}/{max}</span>
      </span>
    );
  };

  return (
    <div className="compare-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="compare-modal">
        {/* Header */}
        <div className="compare-header">
          <h2>⚖️ So sánh ngành học</h2>
          <button className="compare-close" onClick={onClose}>✕</button>
        </div>

        {/* Comparison Table */}
        <div className="compare-table-wrapper">
          <table className="compare-table">
            <thead>
              <tr>
                <th className="criteria-col">Tiêu chí</th>
                <th className={`major-col ${winner === major1.majorId ? 'winner-col' : ''}`}>
                  {winner === major1.majorId && <span className="winner-badge">🏆 AI Recommend</span>}
                  <span className="major-name">{major1.majorName}</span>
                  <span className="field-label">{major1.fieldName}</span>
                </th>
                <th className={`major-col ${winner === major2.majorId ? 'winner-col' : ''}`}>
                  {winner === major2.majorId && <span className="winner-badge">🏆 AI Recommend</span>}
                  <span className="major-name">{major2.majorName}</span>
                  <span className="field-label">{major2.fieldName}</span>
                </th>
              </tr>
            </thead>
            <tbody>
              {/* Match Score */}
              <tr>
                <td className="criteria">🎯 Mức độ phù hợp</td>
                <td>{renderScoreBar(major1.matchScore, winner === major1.majorId)}</td>
                <td>{renderScoreBar(major2.matchScore, winner === major2.majorId)}</td>
              </tr>

              {/* Reason */}
              <tr>
                <td className="criteria">💡 Lý do</td>
                <td className="reason-cell">{major1.reason}</td>
                <td className="reason-cell">{major2.reason}</td>
              </tr>

              {/* Career Paths */}
              <tr>
                <td className="criteria">🚀 Nghề nghiệp</td>
                <td>
                  <div className="tag-list">
                    {major1.careerPaths.map((p, i) => <span key={i} className="tag green">{p}</span>)}
                  </div>
                </td>
                <td>
                  <div className="tag-list">
                    {major2.careerPaths.map((p, i) => <span key={i} className="tag green">{p}</span>)}
                  </div>
                </td>
              </tr>

              {/* Salary */}
              <tr>
                <td className="criteria">💰 Mức lương</td>
                <td className="salary">{major1.salaryRange}</td>
                <td className="salary">{major2.salaryRange}</td>
              </tr>

              {/* Hot Trend */}
              <tr>
                <td className="criteria">🔥 Độ hot</td>
                <td>{renderStars(major1.hotTrendScore)}</td>
                <td>{renderStars(major2.hotTrendScore)}</td>
              </tr>

              {/* AI Resistance */}
              <tr>
                <td className="criteria">🛡️ Kháng AI</td>
                <td>{renderStars(major1.aiResistanceScore)}</td>
                <td>{renderStars(major2.aiResistanceScore)}</td>
              </tr>

              {/* Key Subjects */}
              <tr>
                <td className="criteria">📚 Môn học chính</td>
                <td>
                  <div className="tag-list">
                    {major1.keySubjects.map((s, i) => <span key={i} className="tag blue">{s}</span>)}
                  </div>
                </td>
                <td>
                  <div className="tag-list">
                    {major2.keySubjects.map((s, i) => <span key={i} className="tag blue">{s}</span>)}
                  </div>
                </td>
              </tr>

              {/* Skills to Improve */}
              <tr>
                <td className="criteria">⚡ Cần phát triển</td>
                <td>
                  <div className="tag-list">
                    {major1.skillsToImprove.map((s, i) => <span key={i} className="tag orange">{s}</span>)}
                  </div>
                </td>
                <td>
                  <div className="tag-list">
                    {major2.skillsToImprove.map((s, i) => <span key={i} className="tag orange">{s}</span>)}
                  </div>
                </td>
              </tr>

              {/* Study Duration */}
              <tr>
                <td className="criteria">⏱️ Thời gian học</td>
                <td>{major1.studyDuration}</td>
                <td>{major2.studyDuration}</td>
              </tr>

              {/* Admission */}
              <tr>
                <td className="criteria">🎓 Đầu vào</td>
                <td>{major1.admissionRequirements}</td>
                <td>{major2.admissionRequirements}</td>
              </tr>
            </tbody>
          </table>
        </div>

        {/* AI Conclusion */}
        <div className="compare-conclusion">
          <div className="conclusion-header">
            <span className="conclusion-icon">🤖</span>
            <h3>Kết luận của AI</h3>
          </div>
          <div className="conclusion-winner">
            <span className="trophy">🏆</span>
            <strong>{result.conclusionMajorName}</strong> phù hợp hơn với bạn
          </div>
          <p className="conclusion-text">{result.aiConclusion}</p>
        </div>

        <button className="compare-done-btn" onClick={onClose}>Đã hiểu, quay lại</button>
      </div>
    </div>
  );
}
