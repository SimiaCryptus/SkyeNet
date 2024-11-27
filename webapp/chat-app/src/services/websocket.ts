import {store} from '../store';
import {Message} from "../types";

export class WebSocketService {
    public ws: WebSocket | null = null;
    private readonly DEBUG = process.env.NODE_ENV === 'development';
    private maxReconnectAttempts = 5;
    private reconnectAttempts = 0;
    private heartbeatInterval: NodeJS.Timeout | null = null;
    private sessionId = '';
    private messageHandlers: ((data: Message) => void)[] = [];
    private connectionHandlers: ((connected: boolean) => void)[] = [];
    private errorHandlers: ((error: Error) => void)[] = [];
    private isReconnecting = false;
    private connectionTimeout: NodeJS.Timeout | null = null;
    private messageQueue: string[] = [];
    private readonly HEARTBEAT_INTERVAL = 30000;

    queueMessage(message: string) {
        console.debug('[WebSocket] Queuing message:', message.substring(0, 100) + (message.length > 100 ? '...' : ''));
        this.messageQueue.push(message);
        this.processMessageQueue();
    }

    public getSessionId(): string {
        console.debug('[WebSocket] Getting session ID:', this.sessionId);
        return this.sessionId;
    }

    public getUrl(): string {
        const config = this.getConfig();
        if (!config) return 'not available';
        const path = this.getWebSocketPath();
        return `${config.protocol}//${config.url}:${config.port}${path}ws`;
    }

    public addErrorHandler(handler: (error: Error) => void): void {
        this.errorHandlers.push(handler);
        console.log('[WebSocket] Error handler added');
    }

    public removeErrorHandler(handler: (error: Error) => void): void {
        this.errorHandlers = this.errorHandlers.filter(h => h !== handler);
        console.log('[WebSocket] Error handler removed');
    }

    send(message: string): void {
        if (this.ws?.readyState === WebSocket.OPEN) {
            this.debugLog('Sending message:',
                message.length > 100 ? message.substring(0, 100) + '...' : message
            );
            this.ws.send(message);
        } else {
            console.warn('[WebSocket] Cannot send message - connection not open');
        }
    }

    public addConnectionHandler(handler: (connected: boolean) => void): void {
        this.connectionHandlers.push(handler);
        console.log('[WebSocket] Connection handler added');
    }

    public removeConnectionHandler(handler: (connected: boolean) => void): void {
        this.connectionHandlers = this.connectionHandlers.filter(h => h !== handler);
        console.log('[WebSocket] Connection handler removed');
    }

    private debugLog(message: string, ...args: any[]) {
        if (this.DEBUG) {
            console.debug(`[WebSocket] ${message}`, ...args);
        }
    }

    public isConnected(): boolean {
        return this.ws?.readyState === WebSocket.OPEN;
    }

    private stopHeartbeat() {
        if (this.heartbeatInterval) {
            clearInterval(this.heartbeatInterval);
            this.heartbeatInterval = null;
            console.log('[WebSocket] Stopped heartbeat monitoring');
        }
    }


    connect(sessionId: string): void {
        try {
            if (!sessionId) {
                throw new Error('[WebSocket] SessionId is required');
            }
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

    private processMessageQueue() {
        if (this.ws?.readyState === WebSocket.OPEN) {
            console.debug(`[WebSocket] Processing message queue (${this.messageQueue.length} messages)`);
            while (this.messageQueue.length > 0) {
                const message = this.messageQueue.shift();
                if (message) this.send(message);
            }
        }
    }

    private getConfig() {
        const state = store.getState();
        // Load from localStorage as fallback if store is not yet initialized
        if (!state.config?.websocket) {
            console.debug('[WebSocket] Config not found in store, checking localStorage');
            try {
                const savedConfig = localStorage.getItem('websocketConfig');
                if (savedConfig) {
                    const config = JSON.parse(savedConfig);
                    console.log('[WebSocket] Using config from localStorage:', config);
                    // Ensure protocol is correct based on window.location.protocol
                    config.protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
                    return config;
                }
            } catch (error) {
                console.error('[WebSocket] Error reading config from localStorage:', error);
            }
        }
        console.debug('[WebSocket] Using default config');
        return {
            url: window.location.hostname,
            port: state.config.websocket.port || window.location.port || '8083',
            protocol: window.location.protocol === 'https:' ? 'wss:' : 'ws:'
        };
    }

    private getWebSocketPath(): string {
        const path = window.location.pathname;
        const strings = path.split('/');
        let wsPath = '/';
        if (strings.length >= 2 && strings[1] !== '' && strings[1] !== 'index.html') {
            wsPath = '/' + strings[1] + '/';
        }
        console.debug(`[WebSocket] Calculated WebSocket path: ${wsPath}`);
        return wsPath;
    }

    private setupEventHandlers(): void {
        if (!this.ws) {
            console.warn('[WebSocket] Cannot setup event handlers - no WebSocket instance');
            return;
        }
        this.debugLog('Setting up event handlers');

        this.ws.onopen = () => {
            console.log('[WebSocket] Connection established successfully');
            this.reconnectAttempts = 0;
            this.isReconnecting = false;
            this.connectionHandlers.forEach(handler => handler(true));
            if (this.connectionTimeout) {
                clearTimeout(this.connectionTimeout);
            }
            console.debug('[WebSocket] Sending initial connect message');
        };
        this.ws.onmessage = (event) => {
            // Only log message receipt in debug mode
            this.debugLog('Message received');
            // Parse message data
            const [id, version, content] = event.data.split(',');
            if (!id || !version) {
                console.warn('[WebSocket] Received malformed message:', event.data);
                return;
            }

            // Enhanced HTML detection and handling
            const isHtml = typeof content === 'string' &&
                (/<[a-z][\s\S]*>/i.test(content));
            // Ignore connect messages
            if (content.includes('"type":"connect"')) {
                console.debug('[WebSocket] Ignoring connect message');
                return;
            }

            if (isHtml) {
                console.debug('[WebSocket] HTML content detected, preserving markup');
            }

            const message: Message = {
                id,
                type: 'response',
                version,
                content,
                isHtml,
                rawHtml: isHtml ? content : null,
                timestamp: Date.now(),
                sanitized: false
            };
            this.messageHandlers.forEach((handler) => handler(message));
        };

        this.ws.onclose = () => {
            console.log('[WebSocket] Connection closed, stopping heartbeat');
            this.stopHeartbeat();
            this.connectionHandlers.forEach(handler => handler(false));
            if (!this.isReconnecting) {
                this.attemptReconnect();
            }
        };

        this.ws.onerror = (error) => {
            console.error('[WebSocket] Error occurred:', error);
            this.errorHandlers.forEach(handler => handler(new Error('WebSocket connection error')));
            if (this.ws?.readyState !== WebSocket.OPEN) {
                this.attemptReconnect();
            }
        };
    }

    private attemptReconnect(): void {
        if (this.isReconnecting) return;

        const maxAttempts = this.maxReconnectAttempts;
        if (this.reconnectAttempts >= maxAttempts) {
            console.error(`[WebSocket] Max reconnection attempts (${this.maxReconnectAttempts}) reached`);
            // Dispatch global error state
            this.errorHandlers.forEach(handler =>
                handler(new Error(`Maximum reconnection attempts (${maxAttempts}) reached`))
            );
            this.isReconnecting = false;
            this.reconnectAttempts = 0;
            return;
        }
        this.isReconnecting = true;
        const delay = Math.min(1000 * Math.pow(1.5, this.reconnectAttempts), 30000);
        console.log(`[WebSocket] Attempting reconnect #${this.reconnectAttempts + 1} in ${delay}ms`);
        // Show reconnection status to user
        this.connectionHandlers.forEach(handler =>
            handler(false)
        );


        setTimeout(() => {
            this.reconnectAttempts++;
            this.connect(this.sessionId);
        }, delay);
    }
}

export default new WebSocketService();