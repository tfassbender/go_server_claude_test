import apiClient from './apiClient';
import { tokenStorage } from '../utils/tokenStorage';

export interface LoginRequest {
  username: string;
  password: string;
}

export interface RegisterRequest {
  username: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  username: string;
  message?: string;
}

const authService = {
  async login(credentials: LoginRequest): Promise<AuthResponse> {
    const response = await apiClient.post<AuthResponse>('/auth/login', credentials);
    const { token, username } = response.data;

    tokenStorage.setToken(token);
    tokenStorage.setUsername(username);

    return response.data;
  },

  async register(credentials: RegisterRequest): Promise<{ message: string }> {
    const response = await apiClient.post('/auth/register', credentials);
    return response.data;
  },

  logout(): void {
    tokenStorage.clear();
  },

  isAuthenticated(): boolean {
    return !!tokenStorage.getToken();
  },

  getCurrentUsername(): string | null {
    return tokenStorage.getUsername();
  }
};

export default authService;
