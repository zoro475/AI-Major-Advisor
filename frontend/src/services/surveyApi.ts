import type {
  SurveyQuestion,
  SurveyQuestionRequest,
  SurveySubmissionRequest,
  SurveySubmissionResponse,
  RecommendationResponse,
  CompareResponse,
  RoadmapResponse,
  ScorecardResponse,
} from '../types/survey';

const API_BASE = 'http://localhost:8080/api';

// ==================== Helper ====================

async function handleResponse<T>(response: Response): Promise<T> {
  if (!response.ok) {
    const errorText = await response.text();
    throw new Error(errorText || `HTTP Error: ${response.status}`);
  }
  return response.json();
}

// ==================== Admin API ====================

export const surveyAdminApi = {
  getAll: async (): Promise<SurveyQuestion[]> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions`);
    return handleResponse<SurveyQuestion[]>(res);
  },

  getById: async (id: number): Promise<SurveyQuestion> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions/${id}`);
    return handleResponse<SurveyQuestion>(res);
  },

  create: async (data: SurveyQuestionRequest): Promise<SurveyQuestion> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<SurveyQuestion>(res);
  },

  update: async (id: number, data: SurveyQuestionRequest): Promise<SurveyQuestion> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions/${id}`, {
      method: 'PUT',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<SurveyQuestion>(res);
  },

  delete: async (id: number): Promise<void> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions/${id}`, {
      method: 'DELETE',
    });
    if (!res.ok) throw new Error(`HTTP Error: ${res.status}`);
  },

  toggle: async (id: number): Promise<SurveyQuestion> => {
    const res = await fetch(`${API_BASE}/admin/survey-questions/${id}/toggle`, {
      method: 'PATCH',
    });
    return handleResponse<SurveyQuestion>(res);
  },
};

// ==================== Student API ====================

export const surveyStudentApi = {
  getQuestions: async (): Promise<SurveyQuestion[]> => {
    const res = await fetch(`${API_BASE}/survey/questions`);
    return handleResponse<SurveyQuestion[]>(res);
  },

  submit: async (data: SurveySubmissionRequest): Promise<SurveySubmissionResponse> => {
    const res = await fetch(`${API_BASE}/survey/submit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(data),
    });
    return handleResponse<SurveySubmissionResponse>(res);
  },

  getHistory: async (email: string): Promise<SurveySubmissionResponse[]> => {
    const res = await fetch(`${API_BASE}/survey/history?email=${encodeURIComponent(email)}`);
    return handleResponse<SurveySubmissionResponse[]>(res);
  },

  getDetail: async (id: number): Promise<SurveySubmissionResponse> => {
    const res = await fetch(`${API_BASE}/survey/history/${id}`);
    return handleResponse<SurveySubmissionResponse>(res);
  },
};

// ==================== Recommendation API ====================

export const recommendationApi = {
  analyze: async (submissionId: number): Promise<RecommendationResponse> => {
    const res = await fetch(`${API_BASE}/recommendations/analyze`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ submissionId }),
    });
    return handleResponse<RecommendationResponse>(res);
  },

  getBySubmissionId: async (submissionId: number): Promise<RecommendationResponse> => {
    const res = await fetch(`${API_BASE}/recommendations/${submissionId}`);
    return handleResponse<RecommendationResponse>(res);
  },

  compare: async (submissionId: number, majorId1: number, majorId2: number): Promise<CompareResponse> => {
    const res = await fetch(`${API_BASE}/recommendations/compare`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ submissionId, majorId1, majorId2 }),
    });
    return handleResponse<CompareResponse>(res);
  },

  getRoadmap: async (submissionId: number, majorId: number): Promise<RoadmapResponse> => {
    const res = await fetch(`${API_BASE}/recommendations/roadmap`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ submissionId, majorId }),
    });
    return handleResponse<RoadmapResponse>(res);
  },

  getScorecard: async (submissionId: number, majorId: number): Promise<ScorecardResponse> => {
    const res = await fetch(`${API_BASE}/recommendations/scorecard`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ submissionId, majorId }),
    });
    return handleResponse<ScorecardResponse>(res);
  },
};
