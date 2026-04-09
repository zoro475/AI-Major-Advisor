import { useState } from 'react';
import type { SurveyQuestion } from '../../../types/survey';
import './StudentInputs.css';

interface Props {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function RatingMatrixInput({ question, value, onChange }: Props) {
  const maxRating = question.maxRating || 5;
  const options: string[] = Array.isArray(question.options) ? question.options : [];

  // Parse stored value: JSON object {"Toán": 4, "Lý": 3}
  let ratings: Record<string, number> = {};
  try {
    ratings = value ? JSON.parse(value) : {};
  } catch {
    ratings = {};
  }

  const [hoverState, setHoverState] = useState<{ item: string; star: number } | null>(null);

  const handleRate = (item: string, star: number) => {
    const updated = { ...ratings, [item]: star };
    onChange(question.id, JSON.stringify(updated));
  };

  return (
    <div className="rating-matrix">
      {options.map(item => {
        const currentRating = ratings[item] || 0;
        const isHovering = hoverState?.item === item;
        const hoverStar = isHovering ? hoverState!.star : 0;

        return (
          <div key={item} className="matrix-row">
            <span className="matrix-label">{item}</span>
            <div className="matrix-stars">
              {Array.from({ length: maxRating }, (_, i) => i + 1).map(star => (
                <button
                  key={star}
                  type="button"
                  className={`star-btn matrix-star ${star <= (hoverStar || currentRating) ? 'filled' : ''}`}
                  onClick={() => handleRate(item, star)}
                  onMouseEnter={() => setHoverState({ item, star })}
                  onMouseLeave={() => setHoverState(null)}
                  aria-label={`${item}: ${star} sao`}
                >
                  <svg viewBox="0 0 24 24" width="24" height="24" fill="currentColor">
                    <path d="M12 2l3.09 6.26L22 9.27l-5 4.87 1.18 6.88L12 17.77l-6.18 3.25L7 14.14 2 9.27l6.91-1.01L12 2z" />
                  </svg>
                </button>
              ))}
              {currentRating > 0 && (
                <span className="matrix-score">{currentRating}/{maxRating}</span>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
