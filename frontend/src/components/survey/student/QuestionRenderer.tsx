import type { SurveyQuestion } from '../../../types/survey';
import { SingleChoiceInput } from './SingleChoiceInput';
import { MultipleChoiceInput } from './MultipleChoiceInput';
import { RatingInput } from './RatingInput';
import { RatingMatrixInput } from './RatingMatrixInput';
import { TextareaInput } from './TextareaInput';

interface QuestionRendererProps {
  question: SurveyQuestion;
  value: string;
  onChange: (questionId: number, value: string) => void;
}

export function QuestionRenderer({ question, value, onChange }: QuestionRendererProps) {
  switch (question.questionType) {
    case 'SINGLE_CHOICE':
      return <SingleChoiceInput question={question} value={value} onChange={onChange} />;
    case 'MULTIPLE_CHOICE':
      return <MultipleChoiceInput question={question} value={value} onChange={onChange} />;
    case 'RATING':
      return <RatingInput question={question} value={value} onChange={onChange} />;
    case 'RATING_MATRIX':
      return <RatingMatrixInput question={question} value={value} onChange={onChange} />;
    case 'TEXTAREA':
      return <TextareaInput question={question} value={value} onChange={onChange} />;
    default:
      return <p className="error-text">Loại câu hỏi không được hỗ trợ</p>;
  }
}
