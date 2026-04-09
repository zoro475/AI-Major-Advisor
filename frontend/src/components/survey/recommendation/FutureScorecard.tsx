import { useState, useEffect } from 'react';
import type { ScorecardResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './FutureScorecard.css';

interface Props {
  submissionId: number;
  majorId: number;
  majorName: string;
  onClose: () => void;
}

export function FutureScorecard({ submissionId, majorId, majorName, onClose }: Props) {
  const [data, setData] = useState<ScorecardResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetch = async () => {
      try {
        setLoading(true);
        const result = await recommendationApi.getScorecard(submissionId, majorId);
        setData(result);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'Lỗi tải scorecard');
      } finally {
        setLoading(false);
      }
    };
    fetch();
  }, [submissionId, majorId]);

  if (loading) {
    return (
      <div className="sc-overlay">
        <div className="sc-modal">
          <div className="sc-loading">
            <div className="sc-spinner"></div>
            <h3>📊 Đang chấm điểm...</h3>
            <p>AI đang đánh giá <strong>{majorName}</strong> theo 6 chỉ số</p>
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="sc-overlay">
        <div className="sc-modal">
          <div className="sc-error">
            <h3>❌ {error || 'Không có dữ liệu'}</h3>
            <button onClick={onClose}>Đóng</button>
          </div>
        </div>
      </div>
    );
  }

  const getScoreColor = (score: number) => {
    if (score >= 85) return '#22c55e';
    if (score >= 70) return '#06b6d4';
    if (score >= 55) return '#8b5cf6';
    if (score >= 40) return '#f59e0b';
    return '#ef4444';
  };

  const getVerdictClass = (verdict: string) => {
    if (verdict.includes('Xuất sắc')) return 'verdict-excellent';
    if (verdict.includes('Tốt')) return 'verdict-good';
    if (verdict.includes('Khá')) return 'verdict-fair';
    return 'verdict-avg';
  };

  return (
    <div className="sc-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="sc-modal">
        {/* Header */}
        <div className="sc-header">
          <h2>📊 Future-Proof Scorecard</h2>
          <button className="sc-close" onClick={onClose}>✕</button>
        </div>
        <div className="sc-major-name">{data.majorName}</div>

        {/* Overall Score */}
        <div className="sc-overall">
          <div className="sc-overall-ring">
            <svg viewBox="0 0 120 120">
              <circle cx="60" cy="60" r="52" className="sc-ring-bg" />
              <circle
                cx="60" cy="60" r="52"
                className="sc-ring-fg"
                style={{
                  strokeDasharray: `${(data.overallScore / 100) * 327} 327`,
                  stroke: getScoreColor(data.overallScore),
                }}
              />
            </svg>
            <div className="sc-overall-text">
              <span className="sc-overall-number">{data.overallScore}</span>
              <span className="sc-overall-label">/100</span>
            </div>
          </div>
          <div className={`sc-verdict ${getVerdictClass(data.overallVerdict)}`}>
            {data.overallVerdict}
          </div>
        </div>

        {/* 6 Metrics */}
        <div className="sc-metrics">
          {data.metrics.map((m, i) => (
            <div key={i} className="sc-metric" style={{ animationDelay: `${i * 0.1}s` }}>
              <div className="sc-metric-top">
                <span className="sc-metric-icon">{m.icon}</span>
                <span className="sc-metric-name">{m.name}</span>
                <span className="sc-metric-score" style={{ color: getScoreColor(m.score) }}>
                  {m.score}
                </span>
              </div>
              <div className="sc-bar-bg">
                <div
                  className="sc-bar-fill"
                  style={{ width: `${m.score}%`, background: getScoreColor(m.score) }}
                ></div>
              </div>
              <div className="sc-metric-bottom">
                <span className="sc-metric-label" style={{ color: getScoreColor(m.score) }}>{m.label}</span>
                <span className="sc-metric-explain">{m.explanation}</span>
              </div>
            </div>
          ))}
        </div>

        {/* AI Insight */}
        <div className="sc-insight">
          <div className="sc-insight-header">
            <span>🤖</span>
            <h3>Nhận xét từ AI</h3>
          </div>
          <p>{data.aiInsight}</p>
        </div>

        <button className="sc-done" onClick={onClose}>Đã hiểu, quay lại</button>
      </div>
    </div>
  );
}
