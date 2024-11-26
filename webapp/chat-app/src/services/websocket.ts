export class WebSocketService {
  private ws: WebSocket | null = null;
  private reconnectAttempts = 0;
  private maxReconnectAttempts = 5;
  private sessionId: string = '';
  private messageHandlers: ((data: any) => void)[] = [];
  private protocol: string = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  private host: string = window.location.hostname;
  private port: string = window.location.port;

  public getSessionId(): string {
    return this.sessionId;
  }

  public isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  connect(sessionId: string): void {
    try {
      this.sessionId = sessionId;
      const path = this.getWebSocketPath();
      this.ws = new WebSocket(`${this.protocol}//${this.host}:${this.port}${path}ws?sessionId=${sessionId}`);
      this.setupEventHandlers();
    } catch (error) {
      console.error('WebSocket connection error:', error);
    }
  }
  private getWebSocketPath(): string {
    const path = window.location.pathname;
    const strings = path.split('/');
    return (strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html')
      ? '/' + strings[1] + '/'
      : '/';
  }
  addMessageHandler(handler: (data: any) => void): void {
    this.messageHandlers.push(handler);
  }
  removeMessageHandler(handler: (data: any) => void): void {
    this.messageHandlers = this.messageHandlers.filter(h => h !== handler);
  }


  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = () => {
      console.log('WebSocket connected');
      this.reconnectAttempts = 0;
    };
    this.ws.onmessage = (event) => {
      this.messageHandlers.forEach(handler => handler(event.data));
    };


    this.ws.onclose = () => {
      console.log('WebSocket disconnected');
      this.attemptReconnect();
    };

    this.ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };
  }

  private attemptReconnect(): void {
    if (this.reconnectAttempts >= this.maxReconnectAttempts) {
      console.error('Max reconnection attempts reached');
      return;
    }

    setTimeout(() => {
      this.reconnectAttempts++;
      this.connect(this.sessionId);
    }, 1000 * Math.pow(2, this.reconnectAttempts));
  }

  send(message: string): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      this.ws.send(message);
    }
  }

  disconnect(): void {
    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }
  }
}

export default new WebSocketService();