import { useEffect } from 'react';
import sseService, { GameEvent } from '../services/sseService';

export const useGameEvents = (gameId: string | undefined, onEvent: (event: GameEvent) => void) => {
  useEffect(() => {
    if (!gameId) return;

    // Connect to SSE stream
    sseService.connect(gameId, onEvent);

    // Cleanup on unmount
    return () => {
      sseService.disconnect(gameId);
    };
  }, [gameId, onEvent]);
};
