import type { SurveyQuestion } from '../../../types/survey';
import './StudentInputs.css';

interface SingleChoiceProps {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function SingleChoiceInput({ question, value, onChange }: SingleChoiceProps) {
  return (
    <div className="single-choice-group">
      {question.options?.map((option, index) => (
        <label key={index} className={`choice-option ${value === option ? 'selected' : ''}`}>
          <input
            type="radio"
            name={`question-${question.id}`}
            value={option}
            checked={value === option}
            onChange={() => onChange(question.id, option)}
          />
          <span className="radio-custom"></span>
          <span className="option-text">{option}</span>
        </label>
      ))}
    </div>
  );
}
