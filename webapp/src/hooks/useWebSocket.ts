import {useEffect, useRef, useState} from 'react';
import {useDispatch} from 'react-redux';
import {addMessage} from '../store/slices/messageSlice';
import WebSocketService from '../services/websocket';
import {debounce} from '../utils/tabHandling';
import {Message} from "../types/messages";

export const useWebSocket = (sessionId: string) => {
    const RECONNECT_MAX_DELAY = 30000;
    const RECONNECT_BASE_DELAY = 1000;
    const CONNECTION_TIMEOUT = 5000;
    // Add connection status tracking with debounce
    const connectionStatus = useRef({attempts: 0, lastAttempt: 0});
    const RECONNECT_DELAY = 1000; // 1 second delay between attempts
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const [isReconnecting, setIsReconnecting] = useState(false);
    const dispatch = useDispatch();
    // Add connection attempt tracking
    const connectionAttemptRef = useRef(0);
    const MAX_RECONNECT_ATTEMPTS = 5;

    useEffect(() => {
        let connectionTimeout: NodeJS.Timeout;
        // Implement exponential backoff for reconnection
        const getReconnectDelay = () => {
            return Math.min(RECONNECT_BASE_DELAY * Math.pow(2, connectionStatus.current.attempts), RECONNECT_MAX_DELAY);
        };
        // Debounce connection attempts
        const attemptConnection = debounce(() => {
            clearTimeout(connectionTimeout);
            const now = Date.now();
            if (now - connectionStatus.current.lastAttempt < RECONNECT_DELAY) {
                return;
            }
            connectionStatus.current.lastAttempt = now;
            connectionStatus.current.attempts++;
            WebSocketService.connect(sessionId);
            connectionTimeout = setTimeout(() => {
                if (!isConnected) {
                    handleError(new Error('Connection timeout'));
                }
            }, CONNECTION_TIMEOUT);
        }, 100);
        console.log('[WebSocket] Initializing hook with sessionId:', sessionId);
        if (!sessionId) {
            console.warn('[WebSocket] No sessionId provided, skipping connection');
            return;
        }
        // Reset connection attempts on new session
        connectionAttemptRef.current = 0;

        const handleMessage = (message: Message) => {
            console.log('[WebSocket] Received message:', message);
            // Ensure message has required fields
            if (!message.id || !message.version) {
                console.warn('[WebSocket] Received message missing required fields:', message);
                return;
            }
            dispatch(addMessage(message));
        };

        const handleConnectionChange = (connected: boolean) => {
            console.log('[WebSocket] Connection status changed:', connected ? 'Connected' : 'Disconnected');
            setIsConnected(connected);
            if (connected) {
                setError(null);
                setIsReconnecting(false);
                connectionAttemptRef.current = 0;
                console.log('[WebSocket] Connection established successfully');
            }
        };
        const handleError = (err: Error) => {
            console.error('[WebSocket] Connection error:', err);
            setError(err);
            if (connectionStatus.current.attempts < MAX_RECONNECT_ATTEMPTS) {
                setTimeout(attemptConnection, getReconnectDelay());
            }
            setIsReconnecting(true);
            console.log('[WebSocket] Attempting to reconnect...');
        };
        console.log('[WebSocket] Setting up event handlers');

        WebSocketService.addMessageHandler(handleMessage);
        WebSocketService.addConnectionHandler(handleConnectionChange);
        WebSocketService.addErrorHandler(handleError);
        console.log('[WebSocket] Initiating connection...');
        WebSocketService.connect(sessionId);

        return () => {
            clearTimeout(connectionTimeout);
            console.log('[WebSocket] Cleaning up WebSocket connection and handlers');
            WebSocketService.removeMessageHandler(handleMessage);
            WebSocketService.removeConnectionHandler(handleConnectionChange);
            WebSocketService.removeErrorHandler(handleError);
            WebSocketService.disconnect();
            console.log('[WebSocket] Cleanup complete');
        };
    }, [sessionId]);

    return {
        error,
        isReconnecting,
        readyState: WebSocketService.ws?.readyState,
        send: (message: string) => {
            console.log('[WebSocket] Attempting to send message:', message);
            return WebSocketService.send(message);
        },
        isConnected
    };
};