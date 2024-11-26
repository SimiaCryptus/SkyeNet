export class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 10;
  private sessionId = '';
  private messageHandlers: ((data: any) => void)[] = [];
  private protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  private host: string = window.location.hostname;
  private port: string = window.location.port;
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
      // Clear any existing connection timeout
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }

      this.sessionId = sessionId;
      const path = this.getWebSocketPath();
      // Only create new connection if not already connected or reconnecting
      if (!this.isConnected() && !this.isReconnecting) {
        this.ws = new WebSocket(
            `${this.protocol}//${this.host}:${this.port}${path}ws?sessionId=${sessionId}`
        );
        this.setupEventHandlers();
        // Set connection timeout
        this.connectionTimeout = setTimeout(() => {
          if (this.ws?.readyState !== WebSocket.OPEN) {
            this.ws?.close();
            this.attemptReconnect();
          }
        }, 5000);
      }
    } catch (error) {
      console.error('WebSocket connection error:', error);
      this.attemptReconnect();
    }
  }

  removeMessageHandler(handler: (data: any) => void): void {
    this.messageHandlers = this.messageHandlers.filter((h) => h !== handler);
  }

  addMessageHandler(handler: (data: any) => void): void {
    this.messageHandlers.push(handler);
  }

  disconnect(): void {
    if (this.ws) {
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }
      this.isReconnecting = false;
      this.ws.close();
      this.ws = null;
    }
  }

  send(message: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message);
    }
  }

  private getWebSocketPath(): string {
    const path = window.location.pathname;
    const strings = path.split('/');
    return strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html'
      ? '/' + strings[1] + '/'
      : '/';
  }

  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
      this.isReconnecting = false;
      if (this.connectionTimeout) {
        clearTimeout(this.connectionTimeout);
      }
    };
    this.ws.onmessage = (event) => {
      this.messageHandlers.forEach((handler) => handler(event.data));
    };

    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      if (!this.isReconnecting) {
        this.attemptReconnect();
      }
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      if (this.ws?.readyState !== WebSocket.OPEN) {
        this.attemptReconnect();
      }
    };
  }

  private attemptReconnect(): void {
    if (this.isReconnecting) return;

    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }
    this.isReconnecting = true;

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect(this.sessionId);
    }, Math.min(1000 * Math.pow(1.5, this.reconnectAttempts), 30000));
  }
}

export default new WebSocketService();