import { useMemo } from 'react';
import type { SurveyQuestion } from '../../../types/survey';
import './StudentInputs.css';

interface MultipleChoiceProps {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function MultipleChoiceInput({ question, value, onChange }: MultipleChoiceProps) {
  const selectedValues: string[] = useMemo(() => {
    if (!value) return [];
    try {
      return JSON.parse(value);
    } catch {
      return [];
    }
  }, [value]);

  const handleToggle = (option: string) => {
    const newValues = selectedValues.includes(option)
      ? selectedValues.filter(v => v !== option)
      : [...selectedValues, option];
    onChange(question.id, JSON.stringify(newValues));
  };

  return (
    <div className="multiple-choice-group">
      {question.options?.map((option, index) => (
        <label key={index} className={`choice-option ${selectedValues.includes(option) ? 'selected' : ''}`}>
          <input
            type="checkbox"
            value={option}
            checked={selectedValues.includes(option)}
            onChange={() => handleToggle(option)}
          />
          <span className="checkbox-custom"></span>
          <span className="option-text">{option}</span>
        </label>
      ))}
    </div>
  );
}
