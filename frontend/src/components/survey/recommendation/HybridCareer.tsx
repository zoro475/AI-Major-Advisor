import { useState, useEffect } from 'react';
import type { HybridCareerResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './HybridCareer.css';

interface Props {
  submissionId: number;
  majorId1: number;
  majorId2: number;
  major1Name: string;
  major2Name: string;
  onClose: () => void;
}

export function HybridCareer({ submissionId, majorId1, majorId2, major1Name, major2Name, onClose }: Props) {
  const [data, setData] = useState<HybridCareerResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        setLoading(true);
        const result = await recommendationApi.getHybridCareer(submissionId, majorId1, majorId2);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Lỗi tải Hybrid Career');
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [submissionId, majorId1, majorId2]);

  if (loading) {
    return (
      <div className="hc-overlay">
        <div className="hc-modal">
          <div className="hc-loading">
            <div className="hc-icon">🔀</div>
            <h3>Đang tạo nghề Hybrid...</h3>
            <p>AI đang kết hợp <strong>{major1Name}</strong> + <strong>{major2Name}</strong></p>
            <div className="hc-loading-bar"><div className="hc-loading-fill"></div></div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="hc-overlay">
        <div className="hc-modal">
          <div className="hc-error">
            <h3>❌ {error || 'Không có dữ liệu'}</h3>
            <button onClick={onClose}>Đóng</button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="hc-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="hc-modal">
        {/* Header */}
        <div className="hc-header">
          <div className="hc-header-title">
            <span>🔀</span>
            <div>
              <h2>Hybrid Career Path</h2>
              <p className="hc-combo">{data.major1Name} <span>×</span> {data.major2Name}</p>
            </div>
          </div>
          <button className="hc-close" onClick={onClose}>✕</button>
        </div>

        <div className="hc-body">
          {/* Career Cards */}
          <div className="hc-careers">
            {data.hybridCareers.map((career, i) => (
              <div key={i} className="hc-career-card">
                <div className="hc-career-header">
                  <h3>{career.careerTitle}</h3>
                  <div className="hc-demand">
                    <span className="hc-demand-label">Nhu cầu</span>
                    <div className="hc-demand-bar">
                      <div className="hc-demand-fill" style={{ width: `${career.demandScore * 10}%` }}></div>
                    </div>
                    <span className="hc-demand-score">{career.demandScore}/10</span>
                  </div>
                </div>

                <p className="hc-career-desc">{career.description}</p>

                <div className="hc-career-details">
                  <div className="hc-detail-row">
                    <span className="hc-detail-icon">💰</span>
                    <span className="hc-detail-value">{career.salaryRange}</span>
                  </div>
                  <div className="hc-detail-row">
                    <span className="hc-detail-icon">📈</span>
                    <span className="hc-detail-value">{career.growthOutlook}</span>
                  </div>
                </div>

                <div className="hc-tags-section">
                  <h4>💪 Kỹ năng cần có</h4>
                  <div className="hc-tags">
                    {career.requiredSkills.map((s, j) => <span key={j} className="hc-tag skill">{s}</span>)}
                  </div>
                </div>

                <div className="hc-tags-section">
                  <h4>🏢 Công ty tuyển</h4>
                  <div className="hc-tags">
                    {career.companies.map((c, j) => <span key={j} className="hc-tag company">{c}</span>)}
                  </div>
                </div>
              </div>
            ))}
          </div>

          {/* AI Summary */}
          {data.aiSummary && (
            <div className="hc-summary">
              🔥 {data.aiSummary}
            </div>
          )}
        </div>

        <div className="hc-footer">
          <button className="hc-done" onClick={onClose}>Đã hiểu, quay lại</button>
        </div>
      </div>
    </div>
  );
}
