import type { SurveyQuestion } from '../../../types/survey';
import './StudentInputs.css';

interface TextareaProps {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function TextareaInput({ question, value, onChange }: TextareaProps) {
  return (
    <div className="textarea-group">
      <textarea
        className="survey-textarea"
        rows={4}
        placeholder="Nhập câu trả lời của bạn..."
        value={value}
        onChange={(e) => onChange(question.id, e.target.value)}
      />
      <div className="char-count">{value.length} ký tự</div>
    </div>
  );
}
