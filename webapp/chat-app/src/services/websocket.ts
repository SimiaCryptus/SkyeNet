import { store } from '../store';

export class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private sessionId = '';
  private messageHandlers: ((data: any) => void)[] = [];

  private getConfig() {
    const state = store.getState();
    // Load from localStorage as fallback if store is not yet initialized
    if (!state.config?.websocket) {
      try {
        const savedConfig = localStorage.getItem('websocketConfig');
        if (savedConfig) {
          console.log('Using WebSocket config from localStorage:', JSON.parse(savedConfig));
          return JSON.parse(savedConfig);
        }
      } catch (error) {
        console.error('Error reading WebSocket config from localStorage:', error);
      }
    }
    return state.config.websocket;
  }
  private isReconnecting = false;
  private connectionTimeout: NodeJS.Timeout | null = null;

  public getSessionId(): string {
    return this.sessionId;
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  connect(sessionId: string): void {
    try {
      console.log(`[WebSocket] Initiating connection with sessionId: ${sessionId}`);
      const config = this.getConfig();
      if (!config) {
        throw new Error('WebSocket configuration not available');
      }

      // Clear any existing connection timeout
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }

      this.sessionId = sessionId;
      const path = this.getWebSocketPath();
      // Only create new connection if not already connected or reconnecting
      if (!this.isConnected() && !this.isReconnecting) {
        const wsUrl = `${config.protocol}//${config.url}:${config.port}${path}ws?sessionId=${sessionId}`;
        console.log(`[WebSocket] Connecting to: ${wsUrl}`);
        this.ws = new WebSocket(wsUrl);
        this.setupEventHandlers();
        // Set connection timeout
        this.connectionTimeout = setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            console.warn('[WebSocket] Connection timeout reached, attempting to reconnect');
            this.ws?.close();
            this.attemptReconnect();
          }
        }, 5000);
      }
    } catch (error) {
      console.error('[WebSocket] Connection error:', error);
      this.attemptReconnect();
    }
  }

  removeMessageHandler(handler: (data: any) => void): void {
    const handlersBeforeRemoval = this.messageHandlers.length;
    this.messageHandlers = this.messageHandlers.filter((h) => h !== handler);
    const handlersAfterRemoval = this.messageHandlers.length;
    console.log(`[WebSocket] Message handler removed. Handlers count: ${handlersAfterRemoval}`);
  }

  addMessageHandler(handler: (data: any) => void): void {
    this.messageHandlers.push(handler);
    console.log(`[WebSocket] New message handler added. Handlers count: ${this.messageHandlers.length}`);
  }

  disconnect(): void {
    if (this.ws) {
      console.log('[WebSocket] Initiating disconnect');
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }
      this.isReconnecting = false;
      this.ws.close();
      this.ws = null;
      console.log('[WebSocket] Disconnected successfully');
    }
  }

  send(message: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.debug('[WebSocket] Sending message:', message.substring(0, 100) + (message.length > 100 ? '...' : ''));
      this.ws.send(message);
    } else {
      console.warn('[WebSocket] Cannot send message - connection not open');
    }
  }

  private getWebSocketPath(): string {
    const path = window.location.pathname;
    const strings = path.split('/');
    const wsPath = strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html'
      ? '/' + strings[1] + '/'
      : '/';
    console.debug(`[WebSocket] Calculated WebSocket path: ${wsPath}`);
    return wsPath;
  }

  private setupEventHandlers(): void {
    if (!this.ws) {
      console.warn('[WebSocket] Cannot setup event handlers - no WebSocket instance');
      return;
    }

    this.ws.onopen = () => {
      console.log('[WebSocket] Connection established successfully');
      this.reconnectAttempts = 0;
      this.isReconnecting = false;
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }
    };
    this.ws.onmessage = (event) => {
      console.debug('[WebSocket] Message received:',
          typeof event.data === 'string'
              ? event.data.substring(0, 100) + (event.data.length > 100 ? '...' : '')
              : 'Binary data');
      this.messageHandlers.forEach((handler) => handler(event.data));
    };

    this.ws.onclose = () => {
      console.log('[WebSocket] Connection closed');
      if (!this.isReconnecting) {
        this.attemptReconnect();
      }
    };

    this.ws.onerror = (error) => {
      console.error('[WebSocket] Error occurred:', error);
      if (this.ws?.readyState !== WebSocket.OPEN) {
        this.attemptReconnect();
      }
    };
  }

  private attemptReconnect(): void {
    if (this.isReconnecting) return;

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error(`[WebSocket] Max reconnection attempts (${this.maxReconnectAttempts}) reached`);
      return;
    }
    this.isReconnecting = true;
    const delay = Math.min(1000 * Math.pow(1.5, this.reconnectAttempts), 30000);
    console.log(`[WebSocket] Attempting reconnect #${this.reconnectAttempts + 1} in ${delay}ms`);

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect(this.sessionId);
    }, delay);
  }
}

export default new WebSocketService();