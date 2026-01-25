import { GameListItem as GameListItemType } from '../../types/Game';
import GameListItem from './GameListItem';
import './GameList.css';

interface GameListProps {
  games: GameListItemType[];
  onAccept?: (gameId: string) => void;
  onDecline?: (gameId: string) => void;
  emptyMessage?: string;
}

const GameList = ({ games, onAccept, onDecline, emptyMessage = 'No games found' }: GameListProps) => {
  if (games.length === 0) {
    return <div className="empty-message">{emptyMessage}</div>;
  }

  return (
    <div className="game-list">
      {games.map((game) => (
        <GameListItem
          key={game.id}
          game={game}
          onAccept={onAccept}
          onDecline={onDecline}
        />
      ))}
    </div>
  );
};

export default GameList;
