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
            // If data is an object with raw data property, use that instead
            const messageData = typeof data === 'object' ? data.data : data;
            if (!messageData || typeof messageData !== 'string') {
                console.warn('[ChatInterface] Invalid message format received:', data);
                return;
            }

            const [id, version, content] = messageData.split(',');
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