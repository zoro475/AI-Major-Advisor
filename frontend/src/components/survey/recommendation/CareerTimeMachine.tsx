import { useState, useEffect } from 'react';
import type { TimeMachineResponse, FutureSnapshot } from '../../../types/survey';
import { recommendationApi } from '../../../services/surveyApi';
import './CareerTimeMachine.css';

interface Props {
  submissionId: number;
  majorId: number;
  majorName: string;
  onClose: () => void;
}

export function CareerTimeMachine({ submissionId, majorId, majorName, onClose }: Props) {
  const [data, setData] = useState<TimeMachineResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [activeTab, setActiveTab] = useState(0);
  const [showWhatIf, setShowWhatIf] = useState(false);
  const [customSkills, setCustomSkills] = useState('');
  const [customInterests, setCustomInterests] = useState('');
  const [isWhatIfLoading, setIsWhatIfLoading] = useState(false);

  const fetchTimeMachine = async (skills?: string[], interests?: string[]) => {
    try {
      setLoading(!skills && !interests);
      setIsWhatIfLoading(!!(skills || interests));
      setError(null);
      const result = await recommendationApi.getTimeMachine(
        submissionId, majorId, skills, interests
      );
      setData(result);
      setActiveTab(0);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Lỗi tải Time Machine');
    } finally {
      setLoading(false);
      setIsWhatIfLoading(false);
    }
  };

  useEffect(() => {
    fetchTimeMachine();
  }, [submissionId, majorId]);

  const handleWhatIf = () => {
    const skills = customSkills.trim() ? customSkills.split(',').map(s => s.trim()).filter(Boolean) : undefined;
    const interests = customInterests.trim() ? customInterests.split(',').map(s => s.trim()).filter(Boolean) : undefined;
    if (skills || interests) {
      fetchTimeMachine(skills, interests);
    }
  };

  if (loading) {
    return (
      <div className="tm-overlay">
        <div className="tm-modal">
          <div className="tm-loading">
            <div className="tm-time-icon">⏳</div>
            <h3>Đang du hành thời gian...</h3>
            <p>AI đang tạo 3 phiên bản tương lai cho <strong>{majorName}</strong></p>
            <div className="tm-loading-bar"><div className="tm-loading-fill"></div></div>
          </div>
        </div>
      </div>
    );
  }

  if (error || !data) {
    return (
      <div className="tm-overlay">
        <div className="tm-modal">
          <div className="tm-error">
            <h3>❌ {error || 'Không có dữ liệu'}</h3>
            <button onClick={onClose}>Đóng</button>
          </div>
        </div>
      </div>
    );
  }

  const snapshot: FutureSnapshot | undefined = data.snapshots[activeTab];
  const tabEmojis = ['🌱', '🚀', '👑'];
  const tabLabels = ['5 năm', '10 năm', '15 năm'];

  return (
    <div className="tm-overlay" onClick={(e) => { if (e.target === e.currentTarget) onClose(); }}>
      <div className="tm-modal">
        {/* Header */}
        <div className="tm-header">
          <div className="tm-header-title">
            <span className="tm-header-icon">⏳</span>
            <div>
              <h2>Career Time Machine</h2>
              <p className="tm-subtitle">{data.majorName}</p>
            </div>
          </div>
          <button className="tm-close" onClick={onClose}>✕</button>
        </div>

        {/* Student Profile */}
        {data.studentProfile && (
          <div className="tm-profile">
            <span>🎯</span> {data.studentProfile}
          </div>
        )}

        {/* Content wrapper */}
        <div className="tm-content-wrapper">
          {/* Main Content */}
          <div className={`tm-main ${showWhatIf ? 'with-sidebar' : ''}`}>
            {/* Tab Navigation */}
            <div className="tm-tabs">
              {data.snapshots.map((snap, i) => (
                <button
                  key={i}
                  className={`tm-tab ${activeTab === i ? 'active' : ''}`}
                  onClick={() => setActiveTab(i)}
                >
                  <span className="tm-tab-emoji">{snap.emoji || tabEmojis[i]}</span>
                  <span className="tm-tab-label">{tabLabels[i]} sau</span>
                  <span className="tm-tab-title">{snap.title}</span>
                </button>
              ))}
            </div>

            {snapshot && (
              <div className="tm-snapshot" key={activeTab}>
                {/* Salary bar */}
                <div className="tm-salary-bar">
                  <span className="tm-salary-label">💰 Mức lương dự kiến</span>
                  <span className="tm-salary-value">{snapshot.salaryRange}</span>
                </div>

                {/* Day in Life */}
                <div className="tm-section">
                  <h3>📖 Một ngày làm việc điển hình</h3>
                  <div className="tm-day-grid">
                    <div className="tm-day-card morning">
                      <div className="tm-day-header">🌅 Buổi sáng</div>
                      <p>{snapshot.dayInLife.morning}</p>
                    </div>
                    <div className="tm-day-card afternoon">
                      <div className="tm-day-header">☀️ Buổi chiều</div>
                      <p>{snapshot.dayInLife.afternoon}</p>
                    </div>
                    <div className="tm-day-card evening">
                      <div className="tm-day-header">🌙 Buổi tối</div>
                      <p>{snapshot.dayInLife.evening}</p>
                    </div>
                  </div>
                  {snapshot.dayInLife.highlight && (
                    <div className="tm-highlight">
                      ✨ <em>{snapshot.dayInLife.highlight}</em>
                    </div>
                  )}
                </div>

                {/* Achievements */}
                {snapshot.achievements.length > 0 && (
                  <div className="tm-section">
                    <h3>🏆 Thành tựu đạt được</h3>
                    <div className="tm-badge-list">
                      {snapshot.achievements.map((a, i) => (
                        <span key={i} className="tm-badge achievement">🎖️ {a}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Opportunities */}
                {snapshot.opportunities.length > 0 && (
                  <div className="tm-section">
                    <h3>🚀 Cơ hội nổi bật</h3>
                    <div className="tm-badge-list">
                      {snapshot.opportunities.map((o, i) => (
                        <span key={i} className="tm-badge opportunity">💎 {o}</span>
                      ))}
                    </div>
                  </div>
                )}

                {/* Challenges */}
                {snapshot.challenges.length > 0 && (
                  <div className="tm-section">
                    <h3>⚡ Thách thức & Cách vượt qua</h3>
                    <div className="tm-challenges">
                      {snapshot.challenges.map((c, i) => (
                        <div key={i} className="tm-challenge-card">
                          <div className="tm-challenge-name">⚠️ {c.name}</div>
                          <p className="tm-challenge-desc">{c.description}</p>
                          <div className="tm-challenge-fix">
                            💡 <strong>Cách vượt qua:</strong> {c.howToOvercome}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {/* Worst Case */}
                {snapshot.worstCase && snapshot.worstCase.scenario && (
                  <div className="tm-section">
                    <h3>💀 Kịch bản xấu — Nếu không phát triển</h3>
                    <div className="tm-worst-case">
                      <div className="tm-wc-scenario">
                        <strong>📉 Kịch bản:</strong> {snapshot.worstCase.scenario}
                      </div>
                      <div className="tm-wc-consequences">
                        <strong>⚠️ Hậu quả:</strong> {snapshot.worstCase.consequences}
                      </div>
                      <div className="tm-wc-tip">
                        <strong>🛡️ Phòng tránh:</strong> {snapshot.worstCase.preventionTip}
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Overall Message */}
            {data.overallMessage && (
              <div className="tm-overall">
                🌟 {data.overallMessage}
              </div>
            )}
          </div>

          {/* What-If Sidebar */}
          {showWhatIf && (
            <div className="tm-sidebar">
              <h3>🔄 What-If Mode</h3>
              <p className="tm-sidebar-desc">Thay đổi kỹ năng hoặc sở thích để xem tương lai khác!</p>

              <div className="tm-sidebar-field">
                <label>💪 Kỹ năng bổ sung</label>
                <textarea
                  placeholder="VD: Python, JavaScript, AI..."
                  value={customSkills}
                  onChange={(e) => setCustomSkills(e.target.value)}
                  rows={3}
                />
              </div>

              <div className="tm-sidebar-field">
                <label>❤️ Sở thích mới</label>
                <textarea
                  placeholder="VD: Thiết kế, Kinh doanh..."
                  value={customInterests}
                  onChange={(e) => setCustomInterests(e.target.value)}
                  rows={3}
                />
              </div>

              <button
                className="tm-whatif-btn"
                onClick={handleWhatIf}
                disabled={isWhatIfLoading || (!customSkills.trim() && !customInterests.trim())}
              >
                {isWhatIfLoading ? '⏳ Đang tính toán...' : '🔮 Xem lại tương lai'}
              </button>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="tm-footer">
          <button
            className={`tm-whatif-toggle ${showWhatIf ? 'active' : ''}`}
            onClick={() => setShowWhatIf(!showWhatIf)}
          >
            🔄 {showWhatIf ? 'Ẩn What-If' : 'What-If Mode'}
          </button>
          <button className="tm-done" onClick={onClose}>Đã hiểu, quay lại</button>
        </div>
      </div>
    </div>
  );
}
