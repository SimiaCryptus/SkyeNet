import {useEffect, useState} from 'react';
import WebSocketService from '../services/websocket';

export const useWebSocket = (sessionId: string) => {
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        console.log(`[WebSocket] Initializing with sessionId: ${sessionId}`);

        const handleConnection = () => {
            console.log('[WebSocket] Connected successfully');
            setIsConnected(true);
        };

        const handleDisconnection = () => {
            console.log('[WebSocket] Disconnected');
            setIsConnected(false);
        };

        WebSocketService.addMessageHandler(handleConnection);
        WebSocketService.addMessageHandler(handleDisconnection);
        WebSocketService.connect(sessionId);

        return () => {
            console.log('[WebSocket] Cleaning up connection');
            WebSocketService.removeMessageHandler(handleConnection);
            WebSocketService.removeMessageHandler(handleDisconnection);
            WebSocketService.disconnect();
        };
    }, [sessionId]);

    return {
        send: (message: string) => {
            console.log('[WebSocket] Sending message:', message);
            return WebSocketService.send(message);
        },
        isConnected
    };
};