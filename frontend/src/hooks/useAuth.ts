import { useState, useEffect } from 'react';
import authService from '../services/authService';

export const useAuth = () => {
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(false);
  const [username, setUsername] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState<boolean>(true);

  useEffect(() => {
    const authenticated = authService.isAuthenticated();
    const currentUsername = authService.getCurrentUsername();

    setIsAuthenticated(authenticated);
    setUsername(currentUsername);
    setIsLoading(false);
  }, []);

  const login = async (username: string, password: string) => {
    const response = await authService.login({ username, password });
    setIsAuthenticated(true);
    setUsername(response.username);
  };

  const logout = () => {
    authService.logout();
    setIsAuthenticated(false);
    setUsername(null);
  };

  return {
    isAuthenticated,
    username,
    isLoading,
    login,
    logout
  };
};
