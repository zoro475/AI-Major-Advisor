export type QuestionType = 'SINGLE_CHOICE' | 'MULTIPLE_CHOICE' | 'RATING' | 'RATING_MATRIX' | 'TEXTAREA';

export interface SurveyQuestion {
  id: number;
  questionType: QuestionType;
  questionText: string;
  options: string[] | null;
  maxRating: number | null;
  displayOrder: number;
  isRequired: boolean;
  isActive: boolean;
  createdAt: string;
}

export interface SurveyQuestionRequest {
  questionText: string;
  questionType: QuestionType;
  options?: string[];
  maxRating?: number;
  displayOrder?: number;
  isRequired?: boolean;
}

export interface AnswerRequest {
  questionId: number;
  answerValue: string;
}

export interface SurveySubmissionRequest {
  studentName: string;
  studentEmail?: string;
  freeTextDescription?: string;
  answers: AnswerRequest[];
}

export interface AnswerResponse {
  id: number;
  questionId: number;
  questionText: string;
  questionType: QuestionType;
  answerValue: string;
  createdAt: string;
}

export interface SurveySubmissionResponse {
  id: number;
  studentName: string;
  studentEmail: string;
  freeTextDescription: string;
  submittedAt: string;
  answers: AnswerResponse[];
}

// ==================== Recommendation Types ====================

export interface RecommendationItemResponse {
  majorId: number;
  majorName: string;
  fieldName: string;
  matchScore: number;
  reason: string;
  careerPaths: string[];
  salaryRange: string;
  skillsToImprove: string[];
}

export interface RecommendationResponse {
  id: number;
  submissionId: number;
  studentName: string;
  summary: string;
  modelUsed: string;
  processingTimeMs: number;
  createdAt: string;
  recommendations: RecommendationItemResponse[];
}

// ==================== Compare Types ====================

export interface MajorCompareDetail {
  majorId: number;
  majorName: string;
  fieldName: string;
  matchScore: number;
  reason: string;
  careerPaths: string[];
  salaryRange: string;
  hotTrendScore: number;
  aiResistanceScore: number;
  keySubjects: string[];
  skillsToImprove: string[];
  studyDuration: string;
  admissionRequirements: string;
}

export interface CompareResponse {
  submissionId: number;
  studentName: string;
  major1: MajorCompareDetail;
  major2: MajorCompareDetail;
  aiConclusion: string;
  conclusionMajorName: string;
  conclusionMajorId: number;
}

// ==================== Roadmap Types ====================

export interface RoadmapPhase {
  title: string;
  period: string;
  description: string;
  skills: string[];
  salaryRange: string;
  positions: string[];
}

export interface CareerBranch {
  name: string;
  description: string;
  tools: string[];
  demandLevel: string;
}

export interface RoadmapResponse {
  majorId: number;
  majorName: string;
  overview: string;
  phases: RoadmapPhase[];
  certifications: string[];
  careerBranches: CareerBranch[];
}

// ==================== Scorecard Types ====================

export interface ScoreMetric {
  name: string;
  icon: string;
  score: number;
  label: string;
  explanation: string;
}

export interface ScorecardResponse {
  majorId: number;
  majorName: string;
  overallScore: number;
  overallVerdict: string;
  metrics: ScoreMetric[];
  aiInsight: string;
}
