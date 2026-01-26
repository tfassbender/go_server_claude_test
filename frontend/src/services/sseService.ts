import { tokenStorage } from '../utils/tokenStorage';

export interface GameEvent {
  type: string;
  data: any;
}

export type EventCallback = (event: GameEvent) => void;

class SSEService {
  private eventSources: Map<string, EventSource> = new Map();

  connect(gameId: string, onEvent: EventCallback): void {
    // Close existing connection if any
    this.disconnect(gameId);

    const token = tokenStorage.getToken();
    if (!token) {
      console.error('No auth token available for SSE connection');
      return;
    }

    // Note: EventSource doesn't support custom headers, so we pass token as query param
    // The backend SseJwtFilter extracts this and sets the Authorization header
    const url = `/api/games/${gameId}/events?token=${encodeURIComponent(token)}`;

    try {
      const eventSource = new EventSource(url);

      eventSource.addEventListener('connected', (e) => {
        console.log('SSE connected:', e.data);
        onEvent({ type: 'connected', data: e.data });
      });

      eventSource.addEventListener('move', (e) => {
        onEvent({ type: 'move', data: JSON.parse(e.data) });
      });

      eventSource.addEventListener('pass', (e) => {
        onEvent({ type: 'pass', data: JSON.parse(e.data) });
      });

      eventSource.addEventListener('gameEnd', (e) => {
        onEvent({ type: 'gameEnd', data: JSON.parse(e.data) });
      });

      eventSource.onerror = (error) => {
        // EventSource has built-in auto-retry for transient errors
        // Only log the error and let it reconnect automatically
        // readyState: 0 = CONNECTING, 1 = OPEN, 2 = CLOSED
        if (eventSource.readyState === EventSource.CLOSED) {
          console.error('SSE connection permanently closed:', error);
          this.eventSources.delete(gameId);
          onEvent({ type: 'disconnected', data: null });
        } else {
          console.log('SSE connection error, will auto-retry:', eventSource.readyState);
        }
      };

      this.eventSources.set(gameId, eventSource);
      console.log(`SSE connection established for game ${gameId}`);
    } catch (error) {
      console.error('Error creating SSE connection:', error);
    }
  }

  disconnect(gameId: string): void {
    const eventSource = this.eventSources.get(gameId);
    if (eventSource) {
      eventSource.close();
      this.eventSources.delete(gameId);
      console.log(`SSE connection closed for game ${gameId}`);
    }
  }

  disconnectAll(): void {
    this.eventSources.forEach((es, gameId) => {
      es.close();
      console.log(`SSE connection closed for game ${gameId}`);
    });
    this.eventSources.clear();
  }
}

export const sseService = new SSEService();
export default sseService;
