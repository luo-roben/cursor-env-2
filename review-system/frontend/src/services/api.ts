import axios from 'axios';

const api = axios.create({
  baseURL: '/api/v1',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.response.use(
  (response) => response,
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

// Dashboard
export const getDashboardOverview = (tenantId: number = 1) =>
  api.get('/dashboard/overview', { params: { tenantId } });

// Review
export const submitReview = (data: any) =>
  api.post('/review/submit', data);

export const getReviewList = (tenantId: number = 1, page: number = 0, size: number = 20) =>
  api.get('/review', { params: { tenantId, page, size } });

export const getReviewDetail = (id: number, tenantId: number = 1) =>
  api.get(`/review/${id}`, { params: { tenantId } });

// Law Articles
export const getLawArticles = (params?: any) =>
  api.get('/law/articles', { params });

export const createLawArticle = (data: any) =>
  api.post('/law/articles', data);

export const publishLawArticle = (id: number) =>
  api.put(`/law/articles/${id}/publish`);

export const deprecateLawArticle = (id: number) =>
  api.put(`/law/articles/${id}/deprecate`);

// Law Sources
export const getLawSources = (params?: any) =>
  api.get('/law/sources', { params });

export const createLawSource = (data: any) =>
  api.post('/law/sources', data);

// Contract Templates
export const getContractTemplates = (params?: any) =>
  api.get('/templates', { params });

export const getTemplateClauses = (templateId: number) =>
  api.get(`/templates/${templateId}/clauses`);

// Custom Rules
export const getTenantRules = (tenantId: number = 1) =>
  api.get(`/tenant/rules/tenant/${tenantId}`);

export const createTenantRule = (data: any) =>
  api.post('/tenant/rules', data);

export const updateTenantRule = (id: number, data: any) =>
  api.put(`/tenant/rules/${id}`, data);

export const deleteTenantRule = (id: number) =>
  api.delete(`/tenant/rules/${id}`);

// Checklists
export const getChecklists = (params?: any) =>
  api.get('/checklists', { params });

export const createChecklist = (data: any) =>
  api.post('/checklists', data);

export const updateChecklist = (id: number, data: any) =>
  api.put(`/checklists/${id}`, data);

export const deleteChecklist = (id: number) =>
  api.delete(`/checklists/${id}`);

// Cases
export const getCases = (tenantId: number = 1, params?: any) =>
  api.get('/cases', { params: { tenantId, ...params } });

export default api;
