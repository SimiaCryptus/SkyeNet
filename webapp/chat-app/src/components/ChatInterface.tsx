import React, {useEffect, useState} from 'react';
import {useDispatch} from 'react-redux';
import styled from 'styled-components';
import {useWebSocket} from '../hooks/useWebSocket';
import {addMessage} from '../store/slices/messageSlice';
import MessageList from './MessageList';
import InputArea from './InputArea';
import websocket from '@services/websocket';

interface WebSocketMessage {
    data: string;
    isHtml: boolean;
    timestamp: number;
}

interface ChatInterfaceProps {
    sessionId?: string;
    websocket: typeof websocket;
    isConnected: boolean;
}

const ChatContainer = styled.div`
    display: flex;
    flex-direction: column;
    height: 100vh;
`;

const ChatInterface: React.FC<ChatInterfaceProps> = ({
                                                         sessionId: propSessionId,
                                                         websocket,
                                                         isConnected,
                                                     }) => {
    console.log('[ChatInterface] Rendering with props:', {propSessionId, isConnected});
    const [sessionId] = useState(() => propSessionId || window.location.hash.slice(1) || 'new');
    const dispatch = useDispatch();
    const ws = useWebSocket(sessionId);

    useEffect(() => {
        console.log('[ChatInterface] Setting up message handler for sessionId:', sessionId);

        const handleMessage = (data: WebSocketMessage) => {
            console.log('[ChatInterface] Received message:', data);
            // Handle HTML messages differently
            if (data.isHtml) {
                console.debug('[ChatInterface] Processing HTML message');
                dispatch(addMessage({
                    id: `${Date.now()}`,
                    content: data.data,
                    type: 'response',
                    timestamp: data.timestamp,
                    isHtml: true,
                    rawHtml: data.data,
                    version: '1.0',
                    sanitized: false
                }));
                return;
            }
            // Handle regular messages
            if (!data.data || typeof data.data !== 'string') {
                console.warn('[ChatInterface] Invalid message format received:', data);
                return;
            }
            // Ignore connect messages
            if (data.data.includes('"type":"connect"')) {
                console.debug('[ChatInterface] Ignoring connect message');
                return;
            }

            const [id, version, content] = data.data.split(',');
            const timestamp = Date.now();
            const messageObject = {
                id: `${id}-${timestamp}`,
                content: content,
                version,
                type: id.startsWith('u') ? 'user' as const : 'response' as const,
                timestamp,
            };
            console.log('[ChatInterface] Dispatching message:', messageObject);

            dispatch(addMessage(messageObject));
        };

        websocket.addMessageHandler(handleMessage);
        return () => {
            console.log('[ChatInterface] Cleaning up message handler for sessionId:', sessionId);
            websocket.removeMessageHandler(handleMessage);
        };
    }, [dispatch, ws]);
    const handleSendMessage = (msg: string) => {
        console.log('[ChatInterface] Sending message:', msg);
        ws.send(msg);
    };


    return (
        <ChatContainer>
            <MessageList/>
            <InputArea onSendMessage={handleSendMessage}/>
        </ChatContainer>
    );
};
console.log('[ChatInterface] Component defined');

export default ChatInterface;