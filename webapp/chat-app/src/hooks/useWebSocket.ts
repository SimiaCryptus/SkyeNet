import {useEffect, useState} from 'react';
import WebSocketService from '../services/websocket';

export const useWebSocket = (sessionId: string) => {
    const [isConnected, setIsConnected] = useState(false);

    useEffect(() => {
        const handleConnection = () => {
            setIsConnected(true);
        };
        const handleDisconnection = () => {
            setIsConnected(false);
        };
        WebSocketService.addMessageHandler(handleConnection);
        WebSocketService.connect(sessionId);

        return () => {
            WebSocketService.removeMessageHandler(handleConnection);
            WebSocketService.disconnect();
        };
    }, [sessionId]);

    return {
        send: (message: string) => WebSocketService.send(message),
        isConnected
    };
};