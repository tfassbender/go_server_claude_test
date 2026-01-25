export interface User {
  username: string;
  statistics: UserStatistics;
  createdAt?: string;
}

export interface UserStatistics {
  gamesPlayed: number;
  wins: number;
  losses: number;
}
