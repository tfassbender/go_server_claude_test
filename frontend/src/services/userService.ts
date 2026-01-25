import apiClient from './apiClient';
import { User } from '../types/User';

const userService = {
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<User>('/users/me');
    return response.data;
  },

  async getUserProfile(username: string): Promise<User> {
    const response = await apiClient.get<User>(`/users/${username}`);
    return response.data;
  },

  async searchUsers(query: string = ''): Promise<string[]> {
    const response = await apiClient.get<{ users: string[] }>(`/users/search?q=${encodeURIComponent(query)}`);
    return response.data.users;
  }
};

export default userService;
