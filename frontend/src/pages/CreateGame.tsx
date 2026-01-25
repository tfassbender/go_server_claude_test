import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import gameService from '../services/gameService';
import userService from '../services/userService';
import './CreateGame.css';

const CreateGame = () => {
  const navigate = useNavigate();
  const [opponentUsername, setOpponentUsername] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<string[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<string[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [boardSize, setBoardSize] = useState<9 | 13 | 19>(19);
  const [requestedColor, setRequestedColor] = useState<'black' | 'white' | 'random'>('black');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Load users on mount
  useEffect(() => {
    const loadUsers = async () => {
      try {
        const allUsers = await userService.searchUsers();
        setUsers(allUsers);
        setFilteredUsers(allUsers);
      } catch (err) {
        console.error('Failed to load users:', err);
      }
    };
    loadUsers();
  }, []);

  // Filter users when search query changes
  useEffect(() => {
    const query = searchQuery.toLowerCase();
    const filtered = users.filter(user => user.toLowerCase().includes(query));
    setFilteredUsers(filtered);
  }, [searchQuery, users]);

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

  const handleSelectUser = (username: string) => {
    setOpponentUsername(username);
    setSearchQuery(username);
    setShowDropdown(false);
  };

  const handleSearchChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const value = e.target.value;
    setSearchQuery(value);
    setOpponentUsername(''); // Clear selection when typing
    setShowDropdown(true);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!opponentUsername) {
      setError('Please select an opponent from the list');
      return;
    }

    setLoading(true);
    setError('');

    try {
      const game = await gameService.createGame({
        opponentUsername,
        boardSize,
        requestedColor
      });

      navigate(`/game/${game.id}`);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create game');
      setLoading(false);
    }
  };

  return (
    <div className="container">
      <div className="create-game-page">
        <div className="card create-game-card">
          <h1>Create New Game</h1>

          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label htmlFor="opponent">Opponent</label>
              <div className="user-search-dropdown" ref={dropdownRef}>
                <input
                  id="opponent"
                  type="text"
                  value={searchQuery}
                  onChange={handleSearchChange}
                  onFocus={() => setShowDropdown(true)}
                  required
                  disabled={loading}
                  placeholder="Search for opponent..."
                  autoComplete="off"
                />
                {showDropdown && filteredUsers.length > 0 && (
                  <ul className="dropdown-list">
                    {filteredUsers.map(user => (
                      <li
                        key={user}
                        onClick={() => handleSelectUser(user)}
                        className={user === opponentUsername ? 'selected' : ''}
                      >
                        {user}
                      </li>
                    ))}
                  </ul>
                )}
                {showDropdown && searchQuery && filteredUsers.length === 0 && (
                  <div className="dropdown-empty">No users found</div>
                )}
              </div>
              {opponentUsername && (
                <div className="selected-opponent">
                  Selected: <strong>{opponentUsername}</strong>
                </div>
              )}
            </div>

            <div className="input-group">
              <label htmlFor="boardSize">Board Size</label>
              <select
                id="boardSize"
                value={boardSize}
                onChange={(e) => setBoardSize(Number(e.target.value) as 9 | 13 | 19)}
                disabled={loading}
              >
                <option value={9}>9x9 (Small)</option>
                <option value={13}>13x13 (Medium)</option>
                <option value={19}>19x19 (Standard)</option>
              </select>
            </div>

            <div className="input-group">
              <label htmlFor="color">Your Color</label>
              <select
                id="color"
                value={requestedColor}
                onChange={(e) => setRequestedColor(e.target.value as 'black' | 'white' | 'random')}
                disabled={loading}
              >
                <option value="black">Black (play first)</option>
                <option value="white">White (play second)</option>
                <option value="random">Random</option>
              </select>
            </div>

            {error && <div className="error">{error}</div>}

            <div className="button-group">
              <button
                type="button"
                onClick={() => navigate('/lobby')}
                className="button button-secondary"
                disabled={loading}
              >
                Cancel
              </button>
              <button
                type="submit"
                className="button button-primary"
                disabled={loading}
              >
                {loading ? 'Creating...' : 'Create Game'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default CreateGame;
