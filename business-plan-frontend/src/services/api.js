import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api';

export const createProject = async (projectData) => {
    const response = await axios.post(`${API_BASE_URL}/projects/create`, projectData);
    return response.data;
};

export const saveAnswer = async (questionId, userAnswer) => {
    const response = await axios.post(`${API_BASE_URL}/projects/answer`, {
        questionId,
        userAnswer
    });
    return response.data;
};

export const expandAnswers = async (projectId) => {
    const response = await axios.post(`${API_BASE_URL}/projects/${projectId}/expand`);
    return response.data;
};

export const generateFinalPlan = async (projectId) => {
    const response = await axios.post(`${API_BASE_URL}/projects/${projectId}/generate`);
    return response.data;
};

export const getProject = async (projectId) => {
    const response = await axios.get(`${API_BASE_URL}/projects/${projectId}`);
    return response.data;
};

export const validateBudget = async (budgetData) => {
    const response = await axios.post(`${API_BASE_URL}/projects/validate-budget`, budgetData);
    return response.data;
};