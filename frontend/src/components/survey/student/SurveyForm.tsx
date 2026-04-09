import { useState, useEffect } from 'react';
import type { SurveyQuestion, SurveySubmissionRequest } from '../../../types/survey';
import { surveyStudentApi } from '../../../services/surveyApi';
import { QuestionRenderer } from './QuestionRenderer';
import { RecommendationResult } from '../recommendation/RecommendationResult';
import './SurveyForm.css';

export function SurveyForm() {
  const [questions, setQuestions] = useState<SurveyQuestion[]>([]);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [studentInfo, setStudentInfo] = useState({
    name: '',
    email: '',
    description: '',
  });
  const [submittedId, setSubmittedId] = useState<number | null>(null);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    surveyStudentApi
      .getQuestions()
      .then(setQuestions)
      .catch(() => setError('Không thể tải câu hỏi. Vui lòng thử lại sau.'))
      .finally(() => setLoading(false));
  }, []);

  const handleAnswerChange = (questionId: number, value: string) => {
    setAnswers(prev => ({ ...prev, [questionId]: value }));
  };

  const validate = (): string | null => {
    if (!studentInfo.name.trim()) return 'Vui lòng nhập họ tên';

    for (const q of questions) {
      if (q.isRequired && (!answers[q.id] || !answers[q.id].trim())) {
        return `Vui lòng trả lời câu hỏi: "${q.questionText}"`;
      }
    }
    return null;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    const validationError = validate();
    if (validationError) {
      setError(validationError);
      return;
    }

    setSubmitting(true);
    setError('');

    try {
      const payload: SurveySubmissionRequest = {
        studentName: studentInfo.name.trim(),
        studentEmail: studentInfo.email.trim() || undefined,
        freeTextDescription: studentInfo.description.trim() || undefined,
        answers: Object.entries(answers)
          .filter(([, val]) => val && val.trim())
          .map(([qId, val]) => ({
            questionId: Number(qId),
            answerValue: val,
          })),
      };
      const result = await surveyStudentApi.submit(payload);
      setSubmittedId(result.id);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra. Vui lòng thử lại.');
    } finally {
      setSubmitting(false);
    }
  };

  // After submit → show AI recommendation
  if (submittedId !== null) {
    return (
      <div className="survey-form-container">
        <RecommendationResult
          submissionId={submittedId}
          studentName={studentInfo.name}
        />
        <div style={{ textAlign: 'center', padding: '1rem 0 2rem' }}>
          <button
            className="btn-submit"
            onClick={() => {
              setSubmittedId(null);
              setAnswers({});
              setStudentInfo({ name: '', email: '', description: '' });
            }}
          >
            📋 Làm khảo sát mới
          </button>
        </div>
      </div>
    );
  }

  if (loading) {
    return (
      <div className="survey-loading">
        <div className="spinner"></div>
        <p>Đang tải câu hỏi khảo sát...</p>
      </div>
    );
  }

  const getQuestionTypeLabel = (type: string) => {
    switch (type) {
      case 'SINGLE_CHOICE': return 'Chọn một';
      case 'MULTIPLE_CHOICE': return 'Chọn nhiều';
      case 'RATING': return 'Đánh giá';
      case 'RATING_MATRIX': return 'Đánh giá từng mục';
      case 'TEXTAREA': return 'Tự luận';
      default: return type;
    }
  };

  return (
    <div className="survey-form-container">
      <div className="survey-header">
        <h1>📋 Khảo sát Hướng nghiệp</h1>
        <p>Hãy trả lời các câu hỏi dưới đây để AI phân tích và đề xuất ngành học phù hợp nhất cho bạn</p>
      </div>

      <form onSubmit={handleSubmit} className="survey-form">
        {/* Student Info */}
        <div className="form-section student-info-section">
          <h2>👤 Thông tin cá nhân</h2>
          <div className="form-grid">
            <div className="form-group">
              <label htmlFor="student-name">
                Họ và tên <span className="required">*</span>
              </label>
              <input
                id="student-name"
                type="text"
                placeholder="Nhập họ tên của bạn"
                value={studentInfo.name}
                onChange={e => setStudentInfo(s => ({ ...s, name: e.target.value }))}
                className="form-input"
              />
            </div>
            <div className="form-group">
              <label htmlFor="student-email">Email</label>
              <input
                id="student-email"
                type="email"
                placeholder="example@email.com"
                value={studentInfo.email}
                onChange={e => setStudentInfo(s => ({ ...s, email: e.target.value }))}
                className="form-input"
              />
            </div>
          </div>
        </div>

        {/* Dynamic Questions */}
        {questions.map((q, index) => (
          <div key={q.id} className="form-section question-card">
            <div className="question-header">
              <span className="question-number">Câu {index + 1}</span>
              <span className={`question-type-badge ${q.questionType.toLowerCase()}`}>
                {getQuestionTypeLabel(q.questionType)}
              </span>
            </div>
            <h3 className="question-text">
              {q.questionText}
              {q.isRequired && <span className="required"> *</span>}
            </h3>
            <QuestionRenderer
              question={q}
              value={answers[q.id] || ''}
              onChange={handleAnswerChange}
            />
          </div>
        ))}

        {/* Free Text */}
        <div className="form-section free-text-section">
          <h2>💬 Mô tả thêm về bản thân</h2>
          <p className="section-subtitle">Chia sẻ thêm về sở thích, mong muốn nghề nghiệp của bạn (tùy chọn)</p>
          <textarea
            className="form-textarea"
            rows={5}
            placeholder="Ví dụ: Em thích làm việc với máy tính, muốn học ngành có nhiều cơ hội việc làm..."
            value={studentInfo.description}
            onChange={e => setStudentInfo(s => ({ ...s, description: e.target.value }))}
          />
        </div>

        {/* Error */}
        {error && (
          <div className="error-banner">
            <span>⚠️</span> {error}
          </div>
        )}

        {/* Submit */}
        <button
          type="submit"
          className="btn-submit"
          disabled={submitting}
        >
          {submitting ? (
            <>
              <span className="spinner-small"></span>
              Đang gửi...
            </>
          ) : (
            '🚀 Gửi khảo sát & Nhận kết quả AI'
          )}
        </button>
      </form>
    </div>
  );
}
