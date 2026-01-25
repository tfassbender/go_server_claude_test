const TOKEN_KEY = 'go_game_token';
const USERNAME_KEY = 'go_game_username';

export const tokenStorage = {
  getToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  },

  setToken(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
  },

  removeToken(): void {
    localStorage.removeItem(TOKEN_KEY);
  },

  getUsername(): string | null {
    return localStorage.getItem(USERNAME_KEY);
  },

  setUsername(username: string): void {
    localStorage.setItem(USERNAME_KEY, username);
  },

  removeUsername(): void {
    localStorage.removeItem(USERNAME_KEY);
  },

  clear(): void {
    this.removeToken();
    this.removeUsername();
  }
};
