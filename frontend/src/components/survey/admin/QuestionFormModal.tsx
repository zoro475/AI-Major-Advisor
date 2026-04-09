import { useState } from 'react';
import type { SurveyQuestion, SurveyQuestionRequest, QuestionType } from '../../../types/survey';
import { surveyAdminApi } from '../../../services/surveyApi';
import './QuestionFormModal.css';

interface Props {
  question: SurveyQuestion | null;
  onSave: () => void;
  onClose: () => void;
}

export function QuestionFormModal({ question, onSave, onClose }: Props) {
  const [form, setForm] = useState<SurveyQuestionRequest>({
    questionText: question?.questionText || '',
    questionType: question?.questionType || 'SINGLE_CHOICE',
    options: question?.options || [''],
    maxRating: question?.maxRating || 5,
    displayOrder: question?.displayOrder || 0,
    isRequired: question?.isRequired ?? true,
  });
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState('');

  const showOptions = ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'RATING_MATRIX'].includes(form.questionType);
  const showRating = ['RATING', 'RATING_MATRIX'].includes(form.questionType);

  const handleTypeChange = (type: QuestionType) => {
    setForm(f => ({
      ...f,
      questionType: type,
      options: ['SINGLE_CHOICE', 'MULTIPLE_CHOICE', 'RATING_MATRIX'].includes(type) ? (f.options?.length ? f.options : ['']) : undefined,
      maxRating: ['RATING', 'RATING_MATRIX'].includes(type) ? (f.maxRating || 5) : undefined,
    }));
  };

  const updateOption = (index: number, value: string) => {
    const newOptions = [...(form.options || [])];
    newOptions[index] = value;
    setForm(f => ({ ...f, options: newOptions }));
  };

  const addOption = () => {
    setForm(f => ({ ...f, options: [...(f.options || []), ''] }));
  };

  const removeOption = (index: number) => {
    if ((form.options?.length || 0) <= 1) return;
    const newOptions = (form.options || []).filter((_, i) => i !== index);
    setForm(f => ({ ...f, options: newOptions }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!form.questionText.trim()) {
      setError('Nội dung câu hỏi không được để trống');
      return;
    }

    if (showOptions) {
      const validOptions = form.options?.filter(o => o.trim()) || [];
      if (validOptions.length < 2) {
        setError('Cần ít nhất 2 lựa chọn');
        return;
      }
    }

    setSaving(true);
    setError('');

    try {
      const payload: SurveyQuestionRequest = {
        ...form,
        questionText: form.questionText.trim(),
        options: showOptions ? form.options?.filter(o => o.trim()) : undefined,
        maxRating: showRating ? form.maxRating : undefined,
      };

      if (question) {
        await surveyAdminApi.update(question.id, payload);
      } else {
        await surveyAdminApi.create(payload);
      }
      onSave();
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Có lỗi xảy ra');
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="modal-overlay" onClick={onClose}>
      <div className="modal-content" onClick={e => e.stopPropagation()}>
        <div className="modal-header">
          <h2>{question ? '✏️ Sửa câu hỏi' : '➕ Tạo câu hỏi mới'}</h2>
          <button className="btn-close" onClick={onClose}>✕</button>
        </div>

        <form onSubmit={handleSubmit} className="question-form">
          {/* Question Type */}
          <div className="form-group">
            <label>Loại câu hỏi</label>
            <div className="type-selector">
              {([
                { value: 'SINGLE_CHOICE', label: '🔘 Chọn một', desc: 'Radio buttons' },
                { value: 'MULTIPLE_CHOICE', label: '☑️ Chọn nhiều', desc: 'Checkboxes' },
                { value: 'RATING', label: '⭐ Đánh giá', desc: 'Star rating' },
                { value: 'RATING_MATRIX', label: '📊 Đánh giá từng mục', desc: 'Sao cho từng môn' },
                { value: 'TEXTAREA', label: '📝 Tự luận', desc: 'Free text' },
              ] as const).map(type => (
                <button
                  key={type.value}
                  type="button"
                  className={`type-btn ${form.questionType === type.value ? 'active' : ''}`}
                  onClick={() => handleTypeChange(type.value)}
                >
                  <span className="type-label">{type.label}</span>
                  <span className="type-desc">{type.desc}</span>
                </button>
              ))}
            </div>
          </div>

          {/* Question Text */}
          <div className="form-group">
            <label htmlFor="question-text">
              Nội dung câu hỏi <span className="required">*</span>
            </label>
            <input
              id="question-text"
              type="text"
              className="form-input"
              placeholder="Ví dụ: Bạn thích lĩnh vực nào nhất?"
              value={form.questionText}
              onChange={e => setForm(f => ({ ...f, questionText: e.target.value }))}
            />
          </div>

          {/* Options */}
          {showOptions && (
            <div className="form-group">
              <label>Danh sách lựa chọn</label>
              <div className="options-editor">
                {form.options?.map((opt, i) => (
                  <div key={i} className="option-row">
                    <span className="option-index">{i + 1}.</span>
                    <input
                      type="text"
                      className="form-input"
                      placeholder={`Lựa chọn ${i + 1}`}
                      value={opt}
                      onChange={e => updateOption(i, e.target.value)}
                    />
                    <button
                      type="button"
                      className="btn-remove-option"
                      onClick={() => removeOption(i)}
                      disabled={(form.options?.length || 0) <= 1}
                    >
                      ✕
                    </button>
                  </div>
                ))}
                <button type="button" className="btn-add-option" onClick={addOption}>
                  + Thêm lựa chọn
                </button>
              </div>
            </div>
          )}

          {/* Max Rating */}
          {showRating && (
            <div className="form-group">
              <label htmlFor="max-rating">Số sao tối đa</label>
              <input
                id="max-rating"
                type="number"
                className="form-input form-input-small"
                min={1}
                max={10}
                value={form.maxRating}
                onChange={e => setForm(f => ({ ...f, maxRating: Number(e.target.value) }))}
              />
            </div>
          )}

          {/* Display Order & Required */}
          <div className="form-row">
            <div className="form-group">
              <label htmlFor="display-order">Thứ tự hiển thị</label>
              <input
                id="display-order"
                type="number"
                className="form-input form-input-small"
                min={0}
                value={form.displayOrder}
                onChange={e => setForm(f => ({ ...f, displayOrder: Number(e.target.value) }))}
              />
            </div>
            <div className="form-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={form.isRequired}
                  onChange={e => setForm(f => ({ ...f, isRequired: e.target.checked }))}
                />
                <span>Bắt buộc trả lời</span>
              </label>
            </div>
          </div>

          {error && (
            <div className="form-error">⚠️ {error}</div>
          )}

          <div className="modal-footer">
            <button type="button" className="btn-cancel" onClick={onClose}>
              Hủy
            </button>
            <button type="submit" className="btn-save" disabled={saving}>
              {saving ? 'Đang lưu...' : question ? 'Cập nhật' : 'Tạo câu hỏi'}
            </button>
          </div>
        </form>
      </div>
    </div>
  );
}
