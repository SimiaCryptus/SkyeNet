import {useEffect, useState} from 'react';
import WebSocketService from '../services/websocket';

export const useWebSocket = (sessionId: string) => {
    useEffect(() => {
        WebSocketService.connect(sessionId);
        return () => {
            WebSocketService.disconnect();
        };
    }, [sessionId]);

    return WebSocketService;
};