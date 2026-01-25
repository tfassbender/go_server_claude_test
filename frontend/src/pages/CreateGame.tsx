import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import gameService from '../services/gameService';
import './CreateGame.css';

const CreateGame = () => {
  const navigate = useNavigate();
  const [opponentUsername, setOpponentUsername] = useState('');
  const [boardSize, setBoardSize] = useState<9 | 13 | 19>(19);
  const [requestedColor, setRequestedColor] = useState<'black' | 'white' | 'random'>('black');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
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
              <label htmlFor="opponent">Opponent Username</label>
              <input
                id="opponent"
                type="text"
                value={opponentUsername}
                onChange={(e) => setOpponentUsername(e.target.value)}
                required
                minLength={3}
                maxLength={20}
                disabled={loading}
                placeholder="Enter opponent's username"
              />
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
