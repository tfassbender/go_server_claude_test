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
  }
};

export default userService;
