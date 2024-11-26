import React, {useEffect, useState} from 'react';
import {useDispatch} from 'react-redux';
import styled from 'styled-components';
import {useWebSocket} from '../hooks/useWebSocket';
import {addMessage} from '../store/slices/messageSlice';
import MessageList from './MessageList';
import InputArea from './InputArea';
import Header from './Header';
import websocket from "@services/websocket";

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

const ChatInterface: React.FC<ChatInterfaceProps> = ({sessionId: propSessionId, websocket, isConnected}) => {
    const [sessionId] = useState(() => propSessionId || window.location.hash.slice(1) || 'new');
    const dispatch = useDispatch();
    const ws = useWebSocket(sessionId);

    useEffect(() => {
        const handleMessage = (data: string) => {
            const [id, version, content] = data.split(',');

            dispatch(addMessage({
                id,
                content: content,
                version,
                type: id.startsWith('u') ? 'user' : 'response',
                timestamp: Date.now()
            }));
        };

        websocket.addMessageHandler(handleMessage);
        return () => websocket.removeMessageHandler(handleMessage);
    }, [dispatch, ws]);

    return (
        <ChatContainer>
            <Header onThemeChange={(theme) => {
            }}/>
            <MessageList/>
            <InputArea onSendMessage={(msg) => ws.send(msg)}/>
        </ChatContainer>
    );
};

export default ChatInterface;