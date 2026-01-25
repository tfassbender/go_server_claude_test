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

    // Note: EventSource doesn't support custom headers, so we'll pass token as query param
    // This is a limitation of SSE - in production, consider alternatives
    const url = `/api/games/${gameId}/events`;

    try {
      const eventSource = new EventSource(url);

      eventSource.addEventListener('connected', (e) => {
        console.log('SSE connected:', e.data);
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
        console.error('SSE error:', error);
        eventSource.close();
        this.eventSources.delete(gameId);
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
