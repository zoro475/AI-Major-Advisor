import { useState, useEffect } from 'react';
import type { RoadmapResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './CareerRoadmap.css';

interface Props {
  submissionId: number;
  majorId: number;
  majorName: string;
  onClose: () => void;
}

export function CareerRoadmap({ submissionId, majorId, majorName, onClose }: Props) {
  const [data, setData] = useState<RoadmapResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        setLoading(true);
        const result = await recommendationApi.getRoadmap(submissionId, majorId);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Lỗi tải lộ trình');
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [submissionId, majorId]);

  if (loading) {
    return (
      <div className="roadmap-overlay">
        <div className="roadmap-modal">
          <div className="roadmap-loading">
            <div className="roadmap-spinner"></div>
            <h3>🗺️ Đang tạo lộ trình...</h3>
            <p>AI đang lên kế hoạch nghề nghiệp cho <strong>{majorName}</strong></p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="roadmap-overlay">
        <div className="roadmap-modal">
          <div className="roadmap-error">
            <h3>❌ {error || 'Không có dữ liệu'}</h3>
            <button onClick={onClose}>Đóng</button>
          </div>
        </div>
      </div>
    );
  }

  const phaseColors = ['#8b5cf6', '#06b6d4', '#22c55e', '#f59e0b'];

  return (
    <div className="roadmap-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="roadmap-modal">
        {/* Header */}
        <div className="roadmap-header">
          <h2>🗺️ Lộ trình nghề nghiệp</h2>
          <button className="roadmap-close" onClick={onClose}>✕</button>
        </div>
        <div className="roadmap-major-name">{data.majorName}</div>
        <p className="roadmap-overview">{data.overview}</p>

        {/* Timeline Phases */}
        <div className="roadmap-timeline">
          {data.phases.map((phase, i) => (
            <div key={i} className="roadmap-phase" style={{ '--phase-color': phaseColors[i % 4] } as React.CSSProperties}>
              <div className="phase-dot">
                <span className="phase-number">{i + 1}</span>
              </div>
              <div className="phase-content">
                <div className="phase-header">
                  <h3>{phase.title}</h3>
                  <span className="phase-period">{phase.period}</span>
                </div>
                <p className="phase-desc">{phase.description}</p>

                <div className="phase-details">
                  <div className="phase-section">
                    <h4>💡 Kỹ năng</h4>
                    <div className="phase-tags">
                      {phase.skills.map((s, j) => <span key={j} className="ptag skill">{s}</span>)}
                    </div>
                  </div>
                  <div className="phase-section">
                    <h4>💼 Vị trí</h4>
                    <div className="phase-tags">
                      {phase.positions.map((p, j) => <span key={j} className="ptag position">{p}</span>)}
                    </div>
                  </div>
                  <div className="phase-salary">
                    <span>💰 {phase.salaryRange}</span>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Certifications */}
        <div className="roadmap-section">
          <h3>🎓 Chứng chỉ gợi ý</h3>
          <div className="cert-list">
            {data.certifications.map((c, i) => (
              <div key={i} className="cert-item">
                <span className="cert-icon">📜</span>
                <span>{c}</span>
              </div>
            ))}
          </div>
        </div>

        {/* Career Branches */}
        <div className="roadmap-section">
          <h3>🔀 Các mảng việc làm</h3>
          <div className="branch-grid">
            {data.careerBranches.map((b, i) => (
              <div key={i} className="branch-card">
                <div className="branch-header">
                  <h4>{b.name}</h4>
                  <span className={`demand-badge ${b.demandLevel.includes('Rất') ? 'hot' : ''}`}>
                    {b.demandLevel}
                  </span>
                </div>
                <p>{b.description}</p>
                <div className="branch-tools">
                  {b.tools.map((t, j) => <span key={j} className="tool-tag">{t}</span>)}
                </div>
              </div>
            ))}
          </div>
        </div>

        <button className="roadmap-done" onClick={onClose}>Đã hiểu, quay lại</button>
      </div>
    </div>
  );
}
