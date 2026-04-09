import { useState } from 'react';
import type { SurveyQuestion } from '../../../types/survey';
import './StudentInputs.css';

interface RatingProps {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function RatingInput({ question, value, onChange }: RatingProps) {
  const maxRating = question.maxRating || 5;
  const currentRating = value ? parseInt(value) : 0;
  const [hoverRating, setHoverRating] = useState(0);

  return (
    <div className="rating-group">
      <div className="stars-container">
        {Array.from({ length: maxRating }, (_, i) => i + 1).map(star => (
          <button
            key={star}
            type="button"
            className={`star-btn ${star <= (hoverRating || currentRating) ? 'filled' : ''}`}
            onClick={() => onChange(question.id, String(star))}
            onMouseEnter={() => setHoverRating(star)}
            onMouseLeave={() => setHoverRating(0)}
            aria-label={`${star} sao`}
          >
            <svg viewBox="0 0 24 24" width="32" height="32" fill="currentColor">
              <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
            </svg>
          </button>
        ))}
      </div>
      {currentRating > 0 && (
        <span className="rating-label">{currentRating}/{maxRating}</span>
      )}
    </div>
  );
}
