import { useState, useEffect } from 'react';
import { useSearchParams } from 'react-router-dom';
import gameService from '../services/gameService';
import { GameListItem } from '../types/Game';
import GameList from '../components/GameList/GameList';
import './GameLobby.css';

const GameLobby = () => {
  const [searchParams, setSearchParams] = useSearchParams();
  const [activeGames, setActiveGames] = useState<GameListItem[]>([]);
  const [pendingGames, setPendingGames] = useState<GameListItem[]>([]);
  const [completedGames, setCompletedGames] = useState<GameListItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  // Get initial tab from URL or default to 'active'
  const tabParam = searchParams.get('tab');
  const initialTab = (tabParam === 'pending' || tabParam === 'completed') ? tabParam : 'active';
  const [activeTab, setActiveTab] = useState<'active' | 'pending' | 'completed'>(initialTab);

  const loadGames = async () => {
    try {
      setLoading(true);
      setError('');

      const [active, pending, completed] = await Promise.all([
        gameService.getGames('active'),
        gameService.getGames('pending'),
        gameService.getGames('completed')
      ]);

      setActiveGames(active);
      setPendingGames(pending);
      setCompletedGames(completed);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to load games');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadGames();
  }, []);

  const handleAcceptGame = async (gameId: string) => {
    try {
      await gameService.acceptGame(gameId);
      await loadGames(); // Reload games
      setActiveTab('active'); // Switch to Active Games tab
      setSearchParams({}); // Clear URL params
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to accept game');
    }
  };

  const handleDeclineGame = async (gameId: string) => {
    try {
      await gameService.declineGame(gameId);
      await loadGames(); // Reload games
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to decline game');
    }
  };

  if (loading) {
    return (
      <div className="container">
        <div className="loading">Loading games...</div>
      </div>
    );
  }

  return (
    <div className="container">
      <div className="lobby-header">
        <h1>My Games</h1>
        <button
          onClick={loadGames}
          className="button button-secondary"
        >
          Refresh
        </button>
      </div>

      {error && <div className="error">{error}</div>}

      <div className="tabs">
        <button
          className={`tab ${activeTab === 'active' ? 'active' : ''}`}
          onClick={() => { setActiveTab('active'); setSearchParams({}); }}
        >
          Active Games ({activeGames.length})
        </button>
        <button
          className={`tab ${activeTab === 'pending' ? 'active' : ''}`}
          onClick={() => { setActiveTab('pending'); setSearchParams({ tab: 'pending' }); }}
        >
          Pending Invitations ({pendingGames.length})
        </button>
        <button
          className={`tab ${activeTab === 'completed' ? 'active' : ''}`}
          onClick={() => { setActiveTab('completed'); setSearchParams({ tab: 'completed' }); }}
        >
          Completed ({completedGames.length})
        </button>
      </div>

      <div className="tab-content">
        {activeTab === 'active' && (
          <GameList
            games={activeGames}
            emptyMessage="No active games. Create a new game to start playing!"
          />
        )}

        {activeTab === 'pending' && (
          <GameList
            games={pendingGames}
            onAccept={handleAcceptGame}
            onDecline={handleDeclineGame}
            emptyMessage="No pending invitations"
          />
        )}

        {activeTab === 'completed' && (
          <GameList
            games={completedGames}
            emptyMessage="No completed games"
          />
        )}
      </div>
    </div>
  );
};

export default GameLobby;
