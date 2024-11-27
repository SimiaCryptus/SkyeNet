import {useEffect, useRef, useState} from 'react';
import {useDispatch} from 'react-redux';
import {addMessage} from '../store/slices/messageSlice';
import WebSocketService from '../services/websocket';
import {Message} from "../types";

export const useWebSocket = (sessionId: string) => {
    const [isConnected, setIsConnected] = useState(false);
    const [error, setError] = useState<Error | null>(null);
    const [isReconnecting, setIsReconnecting] = useState(false);
    const dispatch = useDispatch();
    // Add connection attempt tracking
    const connectionAttemptRef = useRef(0);
    const MAX_RECONNECT_ATTEMPTS = 5;

    useEffect(() => {
        console.log('[WebSocket] Initializing hook with sessionId:', sessionId);
        if (!sessionId) {
            console.warn('[WebSocket] No sessionId provided, skipping connection');
            return;
        }
        // Reset connection attempts on new session
        connectionAttemptRef.current = 0;

        const handleMessage = (message: Message) => {
            console.log('[WebSocket] Received message:', message);
            if (message.isHtml) {
                console.log('[WebSocket] Processing HTML message');
                const htmlMessage = {
                    id: Date.now().toString(),
                    content: message.content,
                    type: 'response' as const,
                    timestamp: message.timestamp,
                    isHtml: true,
                    rawHtml: message.rawHtml,
                    version: '1.0',
                    sanitized: false,
                };
                console.log('[WebSocket] Dispatching HTML message to store:', htmlMessage);
                dispatch(addMessage(htmlMessage));
            } else {
                console.log('[WebSocket] Received non-HTML message, skipping processing');
            }
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
            if (!isConnected) {
                console.warn('[WebSocket] Cannot send message - not connected');
                return;
            }
            return WebSocketService.send(message);
        },
        isConnected
    };
};