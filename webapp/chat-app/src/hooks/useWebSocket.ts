import {useEffect, useState} from 'react';
import {useDispatch} from 'react-redux';
import {addMessage} from '../store/slices/messageSlice';
import WebSocketService from '../services/websocket';
import {Message} from "../types";

export const useWebSocket = (sessionId: string) => {
    const [isConnected, setIsConnected] = useState(false);
    const dispatch = useDispatch();

    useEffect(() => {
        console.debug(`[WebSocket] Initializing with sessionId: ${sessionId}`);
        if (!sessionId) {
            console.debug('[WebSocket] No sessionId provided, skipping connection');
            return;
        }

        const handleMessage = (message: Message) => {
            if (message.isHtml) {
                console.debug('[WebSocket] Processing HTML message');
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
                console.debug('[WebSocket] Dispatching HTML message:', htmlMessage);
                dispatch(addMessage(htmlMessage));
            }
        };

        const handleConnectionChange = (connected: boolean) => {
            console.log('[WebSocket] Disconnected');
            setIsConnected(connected);
        };

        WebSocketService.addMessageHandler(handleMessage);
        WebSocketService.addConnectionHandler(handleConnectionChange);
        WebSocketService.connect(sessionId);

        return () => {
            console.log('[WebSocket] Cleaning up connection');
            WebSocketService.removeMessageHandler(handleMessage);
            WebSocketService.removeConnectionHandler(handleConnectionChange);
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