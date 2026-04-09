import { useState } from 'react';
import type { SurveySubmissionResponse } from '../../../types/survey';
import { surveyStudentApi } from '../../../services/surveyApi';
import { SurveyDetailModal } from './SurveyDetailModal';
import './SurveyHistory.css';

export function SurveyHistory() {
  const [email, setEmail] = useState('');
  const [submissions, setSubmissions] = useState<SurveySubmissionResponse[]>([]);
  const [selectedSubmission, setSelectedSubmission] = useState<SurveySubmissionResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searched, setSearched] = useState(false);
  const [error, setError] = useState('');

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!email.trim()) {
      setError('Vui lòng nhập email');
      return;
    }

    setLoading(true);
    setError('');
    setSearched(true);

    try {
      const data = await surveyStudentApi.getHistory(email.trim());
      setSubmissions(data);
    } catch {
      setError('Không thể tải lịch sử khảo sát');
    } finally {
      setLoading(false);
    }
  };

  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  return (
    <div className="survey-history-container">
      <div className="history-header">
        <h1>📜 Lịch sử khảo sát</h1>
        <p>Nhập email để xem lại các bài khảo sát đã hoàn thành</p>
      </div>

      <form onSubmit={handleSearch} className="search-form">
        <div className="search-input-group">
          <input
            type="email"
            className="form-input search-input"
            placeholder="Nhập email của bạn..."
            value={email}
            onChange={e => setEmail(e.target.value)}
          />
          <button type="submit" className="btn-search" disabled={loading}>
            {loading ? '...' : '🔍 Tìm kiếm'}
          </button>
        </div>
      </form>

      {error && (
        <div className="error-banner">
          <span>⚠️</span> {error}
        </div>
      )}

      {searched && !loading && submissions.length === 0 && (
        <div className="empty-state">
          <p>Không tìm thấy bài khảo sát nào với email này.</p>
        </div>
      )}

      {submissions.length > 0 && (
        <div className="submissions-list">
          {submissions.map(sub => (
            <div key={sub.id} className="submission-card" onClick={() => setSelectedSubmission(sub)}>
              <div className="submission-info">
                <h3>{sub.studentName}</h3>
                <p className="submission-date">📅 {formatDate(sub.submittedAt)}</p>
                <p className="submission-answers-count">
                  📝 {sub.answers.length} câu trả lời
                </p>
              </div>
              <div className="submission-preview">
                {sub.freeTextDescription && (
                  <p className="description-preview">
                    "{sub.freeTextDescription.substring(0, 100)}
                    {sub.freeTextDescription.length > 100 ? '...' : ''}"
                  </p>
                )}
              </div>
              <span className="view-detail-arrow">→</span>
            </div>
          ))}
        </div>
      )}

      {selectedSubmission && (
        <SurveyDetailModal
          submission={selectedSubmission}
          onClose={() => setSelectedSubmission(null)}
        />
      )}
    </div>
  );
}
