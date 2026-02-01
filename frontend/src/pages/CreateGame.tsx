import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import gameService from '../services/gameService';
import userService from '../services/userService';
import './CreateGame.css';

const CreateGame = () => {
  const navigate = useNavigate();
  const [opponentType, setOpponentType] = useState<'human' | 'ai'>('human');
  const [opponentUsername, setOpponentUsername] = useState('');
  const [searchQuery, setSearchQuery] = useState('');
  const [users, setUsers] = useState<string[]>([]);
  const [aiBots, setAiBots] = useState<string[]>([]);
  const [filteredUsers, setFilteredUsers] = useState<string[]>([]);
  const [showDropdown, setShowDropdown] = useState(false);
  const [boardSize, setBoardSize] = useState<9 | 13 | 19>(19);
  const [requestedColor, setRequestedColor] = useState<'black' | 'white' | 'random'>('random');
  const [komi, setKomi] = useState<number>(5.5);
  const [allowUndo, setAllowUndo] = useState<boolean>(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const dropdownRef = useRef<HTMLDivElement>(null);

  // Load users and AI bots on mount
  useEffect(() => {
    const loadData = async () => {
      try {
        const [allUsers, allBots] = await Promise.all([
          userService.searchUsers(),
          userService.getAiBots()
        ]);
        setUsers(allUsers);
        setFilteredUsers(allUsers);
        setAiBots(allBots);
      } catch (err) {
        console.error('Failed to load users:', err);
      }
    };
    loadData();
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

  const handleOpponentTypeChange = (type: 'human' | 'ai') => {
    setOpponentType(type);
    setOpponentUsername('');
    setSearchQuery('');
    setShowDropdown(false);
    if (type === 'human') {
      setAllowUndo(false); // Reset undo option when switching to human
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    if (!opponentUsername) {
      setError('Please select an opponent');
      return;
    }

    setLoading(true);
    setError('');

    try {
      await gameService.createGame({
        opponentUsername,
        boardSize,
        requestedColor,
        komi,
        allowUndo
      });

      // Redirect to lobby - AI games go to active (auto-accepted), human games go to pending
      const tab = opponentType === 'ai' ? 'active' : 'pending';
      navigate(`/lobby?tab=${tab}`);
    } catch (err: any) {
      setError(err.response?.data?.error || 'Failed to create game');
      setLoading(false);
    }
  };

  // Format bot username for display
  const formatBotName = (botUsername: string): string => {
    // Remove "GnuGo-" prefix and format with descriptions
    const difficulty = botUsername.replace('GnuGo-', '');
    if (difficulty === 'Easy') return 'Easy (15-20 kyu)';
    if (difficulty === 'Casual') return 'Casual (12-15 kyu)';
    if (difficulty === 'Medium') return 'Medium (10-12 kyu)';
    if (difficulty === 'Hard') return 'Hard (8-10 kyu)';
    if (difficulty === 'Max') return 'Maximum (5-8 kyu)';
    return difficulty;
  };

  return (
    <div className="container">
      <div className="create-game-page">
        <div className="card create-game-card">
          <h1>Create New Game</h1>

          <form onSubmit={handleSubmit}>
            <div className="input-group">
              <label>Opponent Type</label>
              <div className="opponent-type-toggle">
                <label className="opponent-type-option">
                  <input
                    type="radio"
                    value="human"
                    checked={opponentType === 'human'}
                    onChange={() => handleOpponentTypeChange('human')}
                    disabled={loading}
                  />
                  <div className="opponent-type-card">
                    <div className="opponent-type-icon">👤</div>
                    <div className="opponent-type-label">Human Player</div>
                    <div className="opponent-type-description">Play against another person</div>
                  </div>
                </label>
                <label className="opponent-type-option">
                  <input
                    type="radio"
                    value="ai"
                    checked={opponentType === 'ai'}
                    onChange={() => handleOpponentTypeChange('ai')}
                    disabled={loading}
                  />
                  <div className="opponent-type-card">
                    <div className="opponent-type-icon">🤖</div>
                    <div className="opponent-type-label">AI Opponent</div>
                    <div className="opponent-type-description">Play against computer</div>
                  </div>
                </label>
              </div>
            </div>

            <div className="input-group">
              <label htmlFor="opponent">
                {opponentType === 'human' ? 'Select Human Opponent' : 'Select AI Difficulty'}
              </label>
              {opponentType === 'human' ? (
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
              ) : (
                <select
                  id="opponent"
                  value={opponentUsername}
                  onChange={(e) => setOpponentUsername(e.target.value)}
                  required
                  disabled={loading}
                >
                  <option value="">-- Select Difficulty --</option>
                  {aiBots.map(bot => (
                    <option key={bot} value={bot}>
                      {formatBotName(bot)}
                    </option>
                  ))}
                </select>
              )}
              {opponentUsername && opponentType === 'human' && (
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

            <div className="input-group">
              <label htmlFor="komi">Komi (compensation for White)</label>
              <select
                id="komi"
                value={komi}
                onChange={(e) => setKomi(Number(e.target.value))}
                disabled={loading}
              >
                <option value={0}>0</option>
                <option value={3.5}>3.5</option>
                <option value={4.5}>4.5</option>
                <option value={5.5}>5.5</option>
                <option value={6.5}>6.5</option>
                <option value={7.5}>7.5</option>
                <option value={8.5}>8.5</option>
              </select>
            </div>

            <div className="input-group">
              <label className="checkbox-label">
                <input
                  type="checkbox"
                  checked={allowUndo}
                  onChange={(e) => setAllowUndo(e.target.checked)}
                  disabled={loading || opponentType === 'human'}
                  title={opponentType === 'human' ? 'Undo is only available in AI games' : 'Allow undoing moves during the game'}
                />
                <span className={opponentType === 'human' ? 'disabled-text' : ''}>
                  Allow undo moves {opponentType === 'human' && '(AI games only)'}
                </span>
              </label>
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
