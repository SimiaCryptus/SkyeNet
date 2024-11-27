import React from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {logger} from '../utils/logger';
import {Message} from '../types';
import {updateTabs} from '../utils/tabHandling';
import {handleMessageAction} from '../utils/messageHandling';

const MessageListContainer = styled.div`
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
`;
const MessageContent = styled.div`
    .href-link, .play-button, .regen-button, .cancel-button, .text-submit-button {
        cursor: pointer;
        user-select: none;

        &:hover {
            opacity: 0.8;
        }
    }
`;
const extractMessageAction = (target: HTMLElement): {messageId: string | undefined, action: string | undefined} => {
    // Check for data attributes
    const messageId = target.getAttribute('data-message-id') ?? 
                     target.getAttribute('data-id') ?? 
                     undefined;
    let action = target.getAttribute('data-message-action') ?? 
                 target.getAttribute('data-action') ?? 
                 undefined;
    // Check element classes
    if (!action) {
        if (target.classList.contains('href-link')) action = 'link';
        else if (target.classList.contains('play-button')) action = 'run';
        else if (target.classList.contains('regen-button')) action = 'regen';
        else if (target.classList.contains('cancel-button')) action = 'stop';
        else if (target.classList.contains('text-submit-button')) action = 'text-submit';
    }
    return {messageId, action};
};

const MessageItem = styled.div<{ type: 'user' | 'system' | 'response' }>`
    padding: 0.5rem 1rem;
    border-radius: 8px;
    max-width: 80%;
    align-self: ${({type}) => type === 'user' ? 'flex-end' : 'flex-start'};
    background-color: ${({type}) => {
    switch (type) {
        case 'user':
            return '#007bff';
        case 'system':
            return '#6c757d';
        default:
            return '#f8f9fa';
    }
    }};
    color: ${({type}) => type === 'user' || type === 'system' ? '#fff' : '#212529'};
`;
const handleClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    const {messageId, action} = extractMessageAction(target);
    if (messageId && action) {
        logger.debug('Message action clicked', {messageId, action});
        e.preventDefault();
        e.stopPropagation();
        handleMessageAction(messageId, action);
    }
};

interface MessageListProps {
        messages?: Message[];
    }

    const MessageList: React.FC<MessageListProps> = ({messages: propMessages}) => {
        logger.component('MessageList', 'Rendering component', {hasPropMessages: !!propMessages});

        // Log when component is mounted/unmounted
        React.useEffect(() => {
            logger.component('MessageList', 'Component mounted', {timestamp: new Date().toISOString()});
            return () => {
                logger.component('MessageList', 'Component unmounted', {timestamp: new Date().toISOString()});
            };
        }, []);

        const storeMessages = useSelector((state: RootState) => state.messages.messages);
        // Ensure messages is always an array
        const messages = Array.isArray(propMessages) ? propMessages :
            Array.isArray(storeMessages) ? storeMessages : [];

        React.useEffect(() => {
            logger.debug('MessageList - Messages updated', {
                messageCount: messages.length,
                messages: messages,
                source: propMessages ? 'props' : 'store'
            });
            // Process tabs after messages update
            requestAnimationFrame(() => {
                updateTabs();
            });
        }, [messages]);

        return (
            <MessageListContainer>
                {messages.map((message) => {
                    logger.debug('MessageList - Rendering message', {
                        id: message.id,
                        type: message.type,
                        timestamp: message.timestamp,
                        contentLength: message.content?.length || 0
                    });
                    // Log message render before returning JSX
                    logger.debug('MessageList - Message rendered', {
                        id: message.id,
                        type: message.type
                    });

                    return (
                        <MessageItem key={`${message.id}-${message.timestamp}-${Math.random()}`} type={message.type}>
                            <MessageContent
                                className="message-body"
                                onClick={handleClick}
                                dangerouslySetInnerHTML={{__html: message.content}}
                            />
                        </MessageItem>
                    );
                })}
            </MessageListContainer>
        );
    };


export default MessageList;