import { useState, useEffect, useRef, useMemo } from 'react';
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

  // Opponent filter state
  const [selectedOpponent, setSelectedOpponent] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [showDropdown, setShowDropdown] = useState(false);
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Get initial tab from URL or default to 'active'
  const tabParam = searchParams.get('tab');
  const initialTab = (tabParam === 'pending' || tabParam === 'completed') ? tabParam : 'active';
  const [activeTab, setActiveTab] = useState<'active' | 'pending' | 'completed'>(initialTab);

  // Get unique opponents from all games
  const uniqueOpponents = useMemo(() => {
    const allGames = [...activeGames, ...pendingGames, ...completedGames];
    const opponents = new Set(allGames.map(g => g.opponent));
    return Array.from(opponents).sort((a, b) => a.toLowerCase().localeCompare(b.toLowerCase()));
  }, [activeGames, pendingGames, completedGames]);

  // Filter opponents based on search query
  const filteredOpponents = useMemo(() => {
    const query = searchQuery.toLowerCase();
    return uniqueOpponents.filter(opponent => opponent.toLowerCase().includes(query));
  }, [uniqueOpponents, searchQuery]);

  // Filter games by selected opponent
  const filterGames = (games: GameListItem[]) => {
    if (!selectedOpponent) return games;
    return games.filter(g => g.opponent === selectedOpponent);
  };

  // Close dropdown when clicking outside
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(event.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

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

  const handleSelectOpponent = (opponent: string) => {
    setSelectedOpponent(opponent);
    setSearchQuery(opponent);
    setShowDropdown(false);
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchQuery(value);
    setSelectedOpponent(''); // Clear selection when typing
    setShowDropdown(true);
  };

  const handleClearFilter = () => {
    setSelectedOpponent('');
    setSearchQuery('');
    setShowDropdown(false);
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
          Active Games ({filterGames(activeGames).length}{selectedOpponent ? `/${activeGames.length}` : ''})
        </button>
        <button
          className={`tab ${activeTab === 'pending' ? 'active' : ''}`}
          onClick={() => { setActiveTab('pending'); setSearchParams({ tab: 'pending' }); }}
        >
          Pending Invitations ({filterGames(pendingGames).length}{selectedOpponent ? `/${pendingGames.length}` : ''})
        </button>
        <button
          className={`tab ${activeTab === 'completed' ? 'active' : ''}`}
          onClick={() => { setActiveTab('completed'); setSearchParams({ tab: 'completed' }); }}
        >
          Completed ({filterGames(completedGames).length}{selectedOpponent ? `/${completedGames.length}` : ''})
        </button>
      </div>

      <div className="filter-section">
        <label htmlFor="opponent-filter">Filter by opponent:</label>
        <div className="opponent-filter-dropdown" ref={dropdownRef}>
          <div className="filter-input-wrapper">
            <input
              id="opponent-filter"
              type="text"
              value={searchQuery}
              onChange={handleSearchChange}
              onFocus={() => setShowDropdown(true)}
              placeholder="Search opponent..."
              autoComplete="off"
            />
            {(searchQuery || selectedOpponent) && (
              <button
                type="button"
                className="clear-filter-btn"
                onClick={handleClearFilter}
                title="Clear filter"
              >
                ×
              </button>
            )}
          </div>
          {showDropdown && filteredOpponents.length > 0 && (
            <ul className="dropdown-list">
              {filteredOpponents.map(opponent => (
                <li
                  key={opponent}
                  onClick={() => handleSelectOpponent(opponent)}
                  className={opponent === selectedOpponent ? 'selected' : ''}
                >
                  {opponent}
                </li>
              ))}
            </ul>
          )}
          {showDropdown && searchQuery && filteredOpponents.length === 0 && (
            <div className="dropdown-empty">No opponents found</div>
          )}
        </div>
      </div>

      <div className="tab-content">
        {activeTab === 'active' && (
          <GameList
            games={filterGames(activeGames)}
            emptyMessage={selectedOpponent ? `No active games against ${selectedOpponent}` : "No active games. Create a new game to start playing!"}
          />
        )}

        {activeTab === 'pending' && (
          <GameList
            games={filterGames(pendingGames)}
            onAccept={handleAcceptGame}
            onDecline={handleDeclineGame}
            emptyMessage={selectedOpponent ? `No pending invitations from ${selectedOpponent}` : "No pending invitations"}
          />
        )}

        {activeTab === 'completed' && (
          <GameList
            games={filterGames(completedGames)}
            emptyMessage={selectedOpponent ? `No completed games against ${selectedOpponent}` : "No completed games"}
          />
        )}
      </div>
    </div>
  );
};

export default GameLobby;
