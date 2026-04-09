import { useState, useEffect } from 'react';
import type { SurveyQuestion } from '../../../types/survey';
import { surveyAdminApi } from '../../../services/surveyApi';
import { QuestionFormModal } from './QuestionFormModal';
import './QuestionManager.css';

export function QuestionManager() {
  const [questions, setQuestions] = useState<SurveyQuestion[]>([]);
  const [loading, setLoading] = useState(true);
  const [editingQuestion, setEditingQuestion] = useState<SurveyQuestion | null>(null);
  const [showModal, setShowModal] = useState(false);
  const [error, setError] = useState('');

  const loadQuestions = async () => {
    try {
      const data = await surveyAdminApi.getAll();
      setQuestions(data);
    } catch {
      setError('Không thể tải danh sách câu hỏi');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadQuestions();
  }, []);

  const handleCreate = () => {
    setEditingQuestion(null);
    setShowModal(true);
  };

  const handleEdit = (question: SurveyQuestion) => {
    setEditingQuestion(question);
    setShowModal(true);
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Bạn có chắc muốn xóa câu hỏi này?')) return;
    try {
      await surveyAdminApi.delete(id);
      await loadQuestions();
    } catch {
      setError('Không thể xóa câu hỏi');
    }
  };

  const handleToggle = async (id: number) => {
    try {
      await surveyAdminApi.toggle(id);
      await loadQuestions();
    } catch {
      setError('Không thể thay đổi trạng thái');
    }
  };

  const handleSaved = async () => {
    setShowModal(false);
    setEditingQuestion(null);
    await loadQuestions();
  };

  const getTypeLabel = (type: string) => {
    switch (type) {
      case 'SINGLE_CHOICE': return '🔘 Chọn một';
      case 'MULTIPLE_CHOICE': return '☑️ Chọn nhiều';
      case 'RATING': return '⭐ Đánh giá';
      case 'RATING_MATRIX': return '📊 Đánh giá từng mục';
      case 'TEXTAREA': return '📝 Tự luận';
      default: return type;
    }
  };

  if (loading) {
    return (
      <div className="admin-loading">
        <div className="spinner"></div>
        <p>Đang tải...</p>
      </div>
    );
  }

  return (
    <div className="question-manager">
      <div className="manager-header">
        <div>
          <h1>⚙️ Quản lý câu hỏi khảo sát</h1>
          <p className="subtitle">Tạo và chỉnh sửa câu hỏi khảo sát cho học sinh</p>
        </div>
        <button className="btn-create" onClick={handleCreate}>
          ➕ Tạo câu hỏi mới
        </button>
      </div>

      {error && (
        <div className="error-banner">
          <span>⚠️</span> {error}
          <button onClick={() => setError('')}>✕</button>
        </div>
      )}

      {questions.length === 0 ? (
        <div className="empty-state">
          <p>Chưa có câu hỏi nào. Hãy tạo câu hỏi đầu tiên!</p>
        </div>
      ) : (
        <div className="questions-table-wrapper">
          <table className="questions-table">
            <thead>
              <tr>
                <th>#</th>
                <th>Câu hỏi</th>
                <th>Loại</th>
                <th>Bắt buộc</th>
                <th>Trạng thái</th>
                <th>Thứ tự</th>
                <th>Hành động</th>
              </tr>
            </thead>
            <tbody>
              {questions.map((q, index) => (
                <tr key={q.id} className={!q.isActive ? 'inactive-row' : ''}>
                  <td>{index + 1}</td>
                  <td className="question-text-cell">
                    <span>{q.questionText}</span>
                    {q.options && q.options.length > 0 && (
                      <div className="options-preview">
                        {q.options.slice(0, 3).join(', ')}
                        {q.options.length > 3 && ` +${q.options.length - 3}`}
                      </div>
                    )}
                  </td>
                  <td>
                    <span className={`type-badge ${q.questionType.toLowerCase()}`}>
                      {getTypeLabel(q.questionType)}
                    </span>
                  </td>
                  <td>{q.isRequired ? '✅' : '—'}</td>
                  <td>
                    <button
                      className={`status-toggle ${q.isActive ? 'active' : 'inactive'}`}
                      onClick={() => handleToggle(q.id)}
                    >
                      {q.isActive ? 'Active' : 'Inactive'}
                    </button>
                  </td>
                  <td>{q.displayOrder}</td>
                  <td className="actions-cell">
                    <button className="btn-icon btn-edit" onClick={() => handleEdit(q)} title="Sửa">
                      ✏️
                    </button>
                    <button className="btn-icon btn-delete" onClick={() => handleDelete(q.id)} title="Xóa">
                      🗑️
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {showModal && (
        <QuestionFormModal
          question={editingQuestion}
          onSave={handleSaved}
          onClose={() => {
            setShowModal(false);
            setEditingQuestion(null);
          }}
        />
      )}
    </div>
  );
}
