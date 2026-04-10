import { useState, useEffect } from 'react';
import type { RecommendationResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import { MajorComparison } from './MajorComparison';
import { CareerRoadmap } from './CareerRoadmap';
import { FutureScorecard } from './FutureScorecard';
import { CareerTimeMachine } from './CareerTimeMachine';
import { WhatIfSimulator } from './WhatIfSimulator';
import { HybridCareer } from './HybridCareer';
import './RecommendationResult.css';

interface Props {
  submissionId: number;
  studentName: string;
}

export function RecommendationResult({ submissionId, studentName }: Props) {
  const [result, setResult] = useState<RecommendationResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Compare state
  const [selectedIndexes, setSelectedIndexes] = useState<number[]>([]);
  const [showCompare, setShowCompare] = useState(false);

  // Roadmap, Scorecard, Time Machine, What-If & Hybrid state
  const [roadmapMajor, setRoadmapMajor] = useState<{ id: number; name: string } | null>(null);
  const [scorecardMajor, setScorecardMajor] = useState<{ id: number; name: string } | null>(null);
  const [timeMachineMajor, setTimeMachineMajor] = useState<{ id: number; name: string } | null>(null);
  const [showWhatIf, setShowWhatIf] = useState(false);
  const [hybridMajors, setHybridMajors] = useState<{ id1: number; name1: string; id2: number; name2: string } | null>(null);

  useEffect(() => {
    let ignore = false;
    const analyze = async () => {
      try {
        setLoading(true);
        setError(null);
        const data = await recommendationApi.analyze(submissionId);
        if (!ignore) setResult(data);
      } catch (err) {
        if (!ignore) setError(err instanceof Error ? err.message : 'Lỗi phân tích AI');
      } finally {
        if (!ignore) setLoading(false);
      }
    };
    analyze();
    return () => { ignore = true; };
  }, [submissionId]);

  const toggleSelect = (index: number) => {
    setSelectedIndexes(prev => {
      if (prev.includes(index)) return prev.filter(i => i !== index);
      if (prev.length >= 2) return [prev[1], index];
      return [...prev, index];
    });
  };

  const canCompare = selectedIndexes.length === 2;

  if (loading) {
    return (
      <div className="rec-loading">
        <div className="rec-loading-spinner"></div>
        <h3>🤖 AI đang phân tích...</h3>
        <p>Chờ AI phân tích câu trả lời của bạn và tìm ngành học phù hợp nhất.</p>
        <p className="rec-loading-hint">Quá trình này mất khoảng 5-15 giây</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="rec-error">
        <h3>❌ Không thể phân tích</h3>
        <p>{error}</p>
        <button onClick={() => window.location.reload()}>Thử lại</button>
      </div>
    );
  }

  if (!result) return null;

  const getSelectedRecs = () => selectedIndexes.map(i => result.recommendations[i]);
  const getSelectedNames = () => getSelectedRecs().map(r => r?.majorName || '');

  return (
    <div className="rec-container">
      {/* Compare Modal */}
      {showCompare && canCompare && (() => {
        const recs = getSelectedRecs();
        return (
          <MajorComparison
            submissionId={submissionId}
            majorId1={recs[0].majorId}
            majorId2={recs[1].majorId}
            majorName1={recs[0].majorName}
            majorName2={recs[1].majorName}
            onClose={() => setShowCompare(false)}
          />
        );
      })()}

      {/* Roadmap Modal */}
      {roadmapMajor && (
        <CareerRoadmap
          submissionId={submissionId}
          majorId={roadmapMajor.id}
          majorName={roadmapMajor.name}
          onClose={() => setRoadmapMajor(null)}
        />
      )}

      {/* Scorecard Modal */}
      {scorecardMajor && (
        <FutureScorecard
          submissionId={submissionId}
          majorId={scorecardMajor.id}
          majorName={scorecardMajor.name}
          onClose={() => setScorecardMajor(null)}
        />
      )}

      {/* Time Machine Modal */}
      {timeMachineMajor && (
        <CareerTimeMachine
          submissionId={submissionId}
          majorId={timeMachineMajor.id}
          majorName={timeMachineMajor.name}
          onClose={() => setTimeMachineMajor(null)}
        />
      )}

      {/* What-If Simulator Modal */}
      {showWhatIf && (
        <WhatIfSimulator
          submissionId={submissionId}
          onClose={() => setShowWhatIf(false)}
        />
      )}

      {/* Hybrid Career Modal */}
      {hybridMajors && (
        <HybridCareer
          submissionId={submissionId}
          majorId1={hybridMajors.id1}
          majorId2={hybridMajors.id2}
          major1Name={hybridMajors.name1}
          major2Name={hybridMajors.name2}
          onClose={() => setHybridMajors(null)}
        />
      )}

      {/* Header */}
      <div className="rec-header">
        <div className="rec-header-icon">🎯</div>
        <h2>Kết quả phân tích AI</h2>
        <p className="rec-greeting">
          Xin chào <strong>{studentName}</strong>, đây là những ngành học AI đề xuất cho bạn!
        </p>
      </div>

      {/* AI Summary */}
      <div className="rec-summary">
        <div className="rec-summary-icon">💡</div>
        <p>{result.summary}</p>
      </div>

      {/* Compare Hint */}
      <div className="rec-compare-hint">
        <p>
          {canCompare
            ? `✅ Đã chọn ${getSelectedNames().join(' & ')} —`
            : `📌 Chọn 2 ngành để so sánh (${selectedIndexes.length}/2)`
          }
          {canCompare && (
            <button className="btn-compare" onClick={() => setShowCompare(true)}>
              ⚖️ So sánh ngay
            </button>
          )}
        </p>
      </div>

      {/* Recommendations */}
      <div className="rec-list">
        {result.recommendations.map((rec, index) => {
          const isSelected = selectedIndexes.includes(index);
          return (
            <div
              key={index}
              className={`rec-card ${isSelected ? 'rec-card-selected' : ''}`}
              style={{ animationDelay: `${index * 0.15}s` }}
            >
              {/* Select checkbox */}
              <label className="rec-select-box" title="Chọn để so sánh">
                <input
                  type="checkbox"
                  checked={isSelected}
                  onChange={() => toggleSelect(index)}
                />
                <span className="rec-checkmark"></span>
              </label>

              {/* Rank Badge */}
              <div className={`rec-rank rank-${index + 1}`}>
                {index === 0 ? '🥇' : index === 1 ? '🥈' : index === 2 ? '🥉' : `#${index + 1}`}
              </div>

              {/* Card Header */}
              <div className="rec-card-header">
                <div className="rec-card-title">
                  <h3>{rec.majorName}</h3>
                  <span className="rec-field-badge">{rec.fieldName}</span>
                </div>
                <div className="rec-score-ring">
                  <svg viewBox="0 0 36 36" className="rec-score-svg">
                    <path
                      className="rec-score-bg"
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                    <path
                      className="rec-score-fg"
                      strokeDasharray={`${rec.matchScore}, 100`}
                      d="M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                    />
                  </svg>
                  <span className="rec-score-text">{rec.matchScore}%</span>
                </div>
              </div>

              {/* Reason */}
              <div className="rec-reason">
                <p>{rec.reason}</p>
              </div>

              {/* Details Grid */}
              <div className="rec-details">
                <div className="rec-detail-section">
                  <h4>🚀 Cơ hội nghề nghiệp</h4>
                  <div className="rec-tags">
                    {rec.careerPaths.map((path, i) => (
                      <span key={i} className="rec-tag career">{path}</span>
                    ))}
                  </div>
                </div>

                <div className="rec-detail-section">
                  <h4>💰 Mức lương tham khảo</h4>
                  <span className="rec-salary">{rec.salaryRange}</span>
                </div>

                {rec.skillsToImprove.length > 0 && (
                  <div className="rec-detail-section">
                    <h4>📚 Cần phát triển thêm</h4>
                    <div className="rec-tags">
                      {rec.skillsToImprove.map((skill, i) => (
                        <span key={i} className="rec-tag skill">{skill}</span>
                      ))}
                    </div>
                  </div>
                )}
              </div>

              {/* Action Buttons */}
              <div className="rec-actions">
                <button
                  className="rec-action-btn roadmap-btn"
                  onClick={() => setRoadmapMajor({ id: rec.majorId, name: rec.majorName })}
                >
                  🗺️ Lộ trình nghề nghiệp
                </button>
                <button
                  className="rec-action-btn scorecard-btn"
                  onClick={() => setScorecardMajor({ id: rec.majorId, name: rec.majorName })}
                >
                  📊 Future-Proof Score
                </button>
                <button
                  className="rec-action-btn timemachine-btn"
                  onClick={() => setTimeMachineMajor({ id: rec.majorId, name: rec.majorName })}
                >
                  ⏳ Time Machine
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Sticky Compare Button (mobile) */}
      {canCompare && (
        <div className="rec-compare-sticky">
          <button className="btn-compare-sticky" onClick={() => setShowCompare(true)}>
            ⚖️ So sánh {getSelectedNames().join(' vs ')}
          </button>
          <button className="btn-hybrid-sticky" onClick={() => {
            const recs = getSelectedRecs();
            setHybridMajors({ id1: recs[0].majorId, name1: recs[0].majorName, id2: recs[1].majorId, name2: recs[1].majorName });
          }}>
            🔀 Hybrid Career
          </button>
        </div>
      )}

      {/* Global Actions */}
      <div className="rec-global-actions">
        <button className="rec-global-btn whatif-global" onClick={() => setShowWhatIf(true)}>
          🔀 What-If Simulator
        </button>
        <button
          className="rec-global-btn export-pdf-btn"
          onClick={() => window.open(`http://localhost:8080/api/export/pdf/${submissionId}`, '_blank')}
        >
          📄 Tải PDF Hồ sơ nghề nghiệp
        </button>
      </div>

      {/* Footer Meta */}
      <div className="rec-meta">
        <span>🤖 Model: {result.modelUsed}</span>
        <span>⏱️ Phân tích trong {(result.processingTimeMs / 1000).toFixed(1)}s</span>
      </div>
    </div>
  );
}
