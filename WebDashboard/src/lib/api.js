import axios from "axios";
import { auth } from "../firebase";

const API_URL = import.meta.env.VITE_API_URL || "http://localhost:8000/api";

const api = axios.create({
  baseURL: API_URL,
});

// Attach Firebase token to all requests
api.interceptors.request.use(
  async (config) => {
    if (auth && auth.currentUser) {
      const token = await auth.currentUser.getIdToken();
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export const validateRole = async () => {
    const response = await api.get('/auth', { 
        params: { androidId: "web-dashboard" } 
    });
    return response.data;
};

export default api;
