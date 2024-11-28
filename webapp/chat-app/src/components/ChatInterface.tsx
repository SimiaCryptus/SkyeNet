import React, {useEffect, useState} from 'react';
import {useDispatch} from 'react-redux';
import styled from 'styled-components';
import {fetchAppConfig} from '../services/appConfig';
import {useWebSocket} from '../hooks/useWebSocket';
import {addMessage} from '../store/slices/messageSlice';
import MessageList from './MessageList';
import InputArea from './InputArea';
import {Message} from '../types';
import websocket from '@services/websocket';
import {logger} from '../utils/logger';

const LOG_PREFIX = '[ChatInterface]';

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
    const DEBUG = process.env.NODE_ENV === 'development';
    const debugLog = (message: string, data?: any) => {
        if (DEBUG) {
            console.debug(`${LOG_PREFIX} ${message}`, data);
        }
    };
    const [messages, setMessages] = React.useState<Message[]>([]);
    console.log(`${LOG_PREFIX} Rendering with props:`, {
        propSessionId,
        isConnected,
        hashedSessionId: window.location.hash
    });

    const [sessionId] = useState(() => propSessionId || window.location.hash.slice(1) || 'new');
    const dispatch = useDispatch();
    const ws = useWebSocket(sessionId);

    useEffect(() => {
        // Fetch app config when component mounts
        if (sessionId) {
        fetchAppConfig(sessionId).then(config => {
            if (config) {
                logger.info('App config loaded successfully');
            } else {
                logger.warn('Could not load app config, using defaults');
            }
            });
        }
        // Fetch app config when component mounts
        if (sessionId) {
            fetchAppConfig(sessionId).catch(error => {
                logger.error('Failed to fetch app config:', error);
            });
        }
        debugLog('Setting up message handler', {
            sessionId,
            isConnected,
            wsReadyState: ws.readyState
        });
        // Add cleanup flag to prevent state updates after unmount
        let isComponentMounted = true;

        const handleMessage = (data: WebSocketMessage) => {
            if (!isComponentMounted) return;

            if (DEBUG) {
                console.group(`${LOG_PREFIX} Processing message`);
                debugLog('Message data:', data);
            }

            // Handle HTML messages differently
            if (data.isHtml) {
                debugLog('Processing HTML message');
                const newMessage = {
                    id: `${Date.now()}`,
                    content: data.data || '',
                    type: 'response' as const,
                    timestamp: data.timestamp,
                    isHtml: true,
                    rawHtml: data.data,
                    version: data.timestamp.toString(),
                    sanitized: false
                };
                if (isComponentMounted) {
                    setMessages(prev => [...prev, newMessage]);
                }
                dispatch(addMessage(newMessage));
                console.groupEnd();
                return;
            }
            // Handle regular messages
            if (!data.data || typeof data.data !== 'string') {
                console.warn(`${LOG_PREFIX} Invalid message format received:`, data);
                console.groupEnd();
                return;
            }
            // Ignore connect messages
            if (data.data.includes('"type":"connect"')) {
                console.debug(`${LOG_PREFIX} Ignoring connect message`);
                console.groupEnd();
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
            console.log(`${LOG_PREFIX} Dispatching message:`, messageObject);
            console.groupEnd();

            dispatch(addMessage(messageObject));
        };

        websocket.addMessageHandler(handleMessage);
        return () => {
            isComponentMounted = false;
            console.log(`${LOG_PREFIX} Cleaning up message handler`, {
                sessionId,
                isConnected
            });
            websocket.removeMessageHandler(handleMessage);
        };
    }, [dispatch, ws]);

    const handleSendMessage = (msg: string) => {
        console.log(`${LOG_PREFIX} Sending message`, {
            messageLength: msg.length,
            sessionId,
            isConnected
        });
        ws.send(msg);
    };

    return (
        <ChatContainer>
            <MessageList/>
            <InputArea onSendMessage={handleSendMessage}/>
        </ChatContainer>
    );
};
console.debug(`${LOG_PREFIX} Component defined`);

export default ChatInterface;