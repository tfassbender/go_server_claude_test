import { Link } from 'react-router-dom';
import { GameListItem as GameListItemType } from '../../types/Game';
import './GameList.css';

interface GameListItemProps {
  game: GameListItemType;
  onAccept?: (gameId: string) => void;
  onDecline?: (gameId: string) => void;
}

const GameListItem = ({ game, onAccept, onDecline }: GameListItemProps) => {
  const isPending = game.status === 'pending';
  const isMyTurn = game.yourColor === game.currentTurn;
  const lastMoveDate = new Date(game.lastMoveAt).toLocaleString();

  return (
    <div className={`game-item ${isPending ? 'pending' : ''} ${isMyTurn ? 'my-turn' : ''}`}>
      <div className="game-info">
        <div className="game-title">
          <strong>vs {game.opponent}</strong>
          <span className="board-size">{game.boardSize}x{game.boardSize}</span>
        </div>

        <div className="game-details">
          <span className={`color-indicator ${game.yourColor}`}>
            You: {game.yourColor}
          </span>

          {game.status === 'active' && (
            <span className={`turn-indicator ${isMyTurn ? 'your-turn' : ''}`}>
              {isMyTurn ? '🟢 Your turn' : `⚪ ${game.opponent}'s turn`}
            </span>
          )}

          {game.status === 'pending' && (
            <span className="status-badge pending">Pending invitation</span>
          )}

          {game.status === 'completed' && (
            <span className="status-badge completed">Completed</span>
          )}
        </div>

        <div className="game-meta">
          <small>Last activity: {lastMoveDate}</small>
        </div>
      </div>

      <div className="game-actions">
        {isPending && onAccept && onDecline ? (
          <>
            <button onClick={() => onAccept(game.id)} className="button button-primary">
              Accept
            </button>
            <button onClick={() => onDecline(game.id)} className="button button-danger">
              Decline
            </button>
          </>
        ) : (
          <Link to={`/game/${game.id}`} className="button button-primary">
            {game.status === 'completed' ? 'View' : 'Play'}
          </Link>
        )}
      </div>
    </div>
  );
};

export default GameListItem;
