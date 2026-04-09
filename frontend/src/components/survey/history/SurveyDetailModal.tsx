import type { SurveySubmissionResponse } from '../../../types/survey';
import './SurveyHistory.css';

interface Props {
  submission: SurveySubmissionResponse;
  onClose: () => void;
}

export function SurveyDetailModal({ submission, onClose }: Props) {
  const formatDate = (dateStr: string) => {
    return new Date(dateStr).toLocaleString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    });
  };

  const renderAnswerValue = (answer: SurveySubmissionResponse['answers'][0]) => {
    switch (answer.questionType) {
      case 'MULTIPLE_CHOICE':
        try {
          const values: string[] = JSON.parse(answer.answerValue);
          return (
            <div className="answer-tags">
              {values.map((v, i) => (
                <span key={i} className="answer-tag">{v}</span>
              ))}
            </div>
          );
        } catch {
          return <span>{answer.answerValue}</span>;
        }
      case 'RATING':
        const rating = parseInt(answer.answerValue);
        return (
          <div className="answer-rating">
            {Array.from({ length: rating }, (_, i) => (
              <span key={i} className="star filled">⭐</span>
            ))}
            <span className="rating-text">({answer.answerValue})</span>
          </div>
        );
      default:
        return <span className="answer-text">{answer.answerValue}</span>;
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content detail-modal" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>📋 Chi tiết bài khảo sát</h2>
          <button className="btn-close" onClick={onClose}>✕</button>
        </div>

        <div className="detail-info">
          <div className="info-row">
            <span className="info-label">👤 Học sinh:</span>
            <span>{submission.studentName}</span>
          </div>
          <div className="info-row">
            <span className="info-label">📧 Email:</span>
            <span>{submission.studentEmail || '—'}</span>
          </div>
          <div className="info-row">
            <span className="info-label">📅 Thời gian:</span>
            <span>{formatDate(submission.submittedAt)}</span>
          </div>
          {submission.freeTextDescription && (
            <div className="info-row description-row">
              <span className="info-label">💬 Mô tả:</span>
              <p>{submission.freeTextDescription}</p>
            </div>
          )}
        </div>

        <div className="detail-answers">
          <h3>Câu trả lời ({submission.answers.length})</h3>
          {submission.answers.map((answer, index) => (
            <div key={answer.id} className="answer-card">
              <div className="answer-question">
                <span className="answer-number">Câu {index + 1}:</span>
                <span>{answer.questionText}</span>
              </div>
              <div className="answer-value">
                {renderAnswerValue(answer)}
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
