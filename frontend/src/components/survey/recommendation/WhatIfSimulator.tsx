import { useState } from 'react';
import type { WhatIfResponse } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './WhatIfSimulator.css';

interface Props {
  submissionId: number;
  onClose: () => void;
}

export function WhatIfSimulator({ submissionId, onClose }: Props) {
  const [data, setData] = useState<WhatIfResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [addSkills, setAddSkills] = useState('');
  const [removeSkills, setRemoveSkills] = useState('');
  const [newInterests, setNewInterests] = useState('');
  const [newPersonality, setNewPersonality] = useState('');

  const handleSimulate = async () => {
    const add = addSkills.trim() ? addSkills.split(',').map(s => s.trim()).filter(Boolean) : undefined;
    const remove = removeSkills.trim() ? removeSkills.split(',').map(s => s.trim()).filter(Boolean) : undefined;
    const interests = newInterests.trim() ? newInterests.split(',').map(s => s.trim()).filter(Boolean) : undefined;
    const personality = newPersonality.trim() || undefined;

    if (!add && !remove && !interests && !personality) return;

    try {
      setLoading(true);
      setError(null);
      const result = await recommendationApi.getWhatIf(submissionId, add, remove, interests, personality);
      setData(result);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi mô phỏng');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="wi-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="wi-modal">
        {/* Header */}
        <div className="wi-header">
          <div className="wi-header-title">
            <span className="wi-icon">🔀</span>
            <div>
              <h2>What-If Simulator</h2>
              <p>Thay đổi kỹ năng & sở thích — xem ngành nào thay đổi</p>
            </div>
          </div>
          <button className="wi-close" onClick={onClose}>✕</button>
        </div>

        <div className="wi-body">
          {/* Input Panel */}
          <div className="wi-input-panel">
            <div className="wi-input-grid">
              <div className="wi-field">
                <label>➕ Thêm kỹ năng</label>
                <input
                  placeholder="VD: Python, React, AI..."
                  value={addSkills}
                  onChange={(e) => setAddSkills(e.target.value)}
                />
              </div>
              <div className="wi-field">
                <label>➖ Bỏ kỹ năng</label>
                <input
                  placeholder="VD: Vẽ tay, Toán..."
                  value={removeSkills}
                  onChange={(e) => setRemoveSkills(e.target.value)}
                />
              </div>
              <div className="wi-field">
                <label>❤️ Sở thích mới</label>
                <input
                  placeholder="VD: Gaming, AI, Kinh doanh..."
                  value={newInterests}
                  onChange={(e) => setNewInterests(e.target.value)}
                />
              </div>
              <div className="wi-field">
                <label>🧠 Tính cách thay đổi</label>
                <input
                  placeholder="VD: Hướng ngoại hơn, sáng tạo hơn..."
                  value={newPersonality}
                  onChange={(e) => setNewPersonality(e.target.value)}
                />
              </div>
            </div>
            <button
              className="wi-simulate-btn"
              onClick={handleSimulate}
              disabled={loading || (!addSkills.trim() && !removeSkills.trim() && !newInterests.trim() && !newPersonality.trim())}
            >
              {loading ? '⏳ Đang mô phỏng...' : '🔮 Mô phỏng ngay'}
            </button>
          </div>

          {/* Error */}
          {error && <div className="wi-error">❌ {error}</div>}

          {/* Results */}
          {data && (
            <div className="wi-results">
              {/* Profile comparison */}
              <div className="wi-profiles">
                <div className="wi-profile-card original">
                  <h4>📋 Profile gốc</h4>
                  <p>{data.originalProfile}</p>
                </div>
                <div className="wi-profile-arrow">→</div>
                <div className="wi-profile-card whatif">
                  <h4>✨ Profile mới</h4>
                  <p>{data.whatIfProfile}</p>
                </div>
              </div>

              {/* Changes table */}
              <div className="wi-changes">
                <h3>📊 Thay đổi matchScore theo ngành</h3>
                <div className="wi-change-list">
                  {data.changes.map((change, i) => (
                    <div key={i} className="wi-change-row">
                      <div className="wi-change-name">{change.majorName}</div>
                      <div className="wi-change-scores">
                        <span className="wi-score old">{change.originalScore}</span>
                        <span className="wi-arrow">→</span>
                        <span className="wi-score new">{change.newScore}</span>
                        <span className={`wi-delta ${change.scoreDelta > 0 ? 'up' : change.scoreDelta < 0 ? 'down' : 'neutral'}`}>
                          {change.scoreDelta > 0 ? '+' : ''}{change.scoreDelta}
                        </span>
                      </div>
                      <div className="wi-change-bar">
                        <div className="wi-bar-old" style={{ width: `${change.originalScore}%` }}></div>
                        <div className="wi-bar-new" style={{ width: `${change.newScore}%` }}></div>
                      </div>
                      <p className="wi-change-reason">{change.changeReason}</p>
                    </div>
                  ))}
                </div>
              </div>

              {/* AI Analysis */}
              {data.aiAnalysis && (
                <div className="wi-analysis">
                  <h3>🤖 Phân tích AI</h3>
                  <p>{data.aiAnalysis}</p>
                </div>
              )}
            </div>
          )}
        </div>

        <div className="wi-footer">
          <button className="wi-done" onClick={onClose}>Đã hiểu, quay lại</button>
        </div>
      </div>
    </div>
  );
}
