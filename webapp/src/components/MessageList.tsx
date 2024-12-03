import React, {useEffect, useRef} from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {useTheme} from '../hooks/useTheme';
import {RootState} from '../store';

import {resetTabState, updateTabs} from '../utils/tabHandling';
import WebSocketService from "../services/websocket";
import Prism from 'prismjs';
import {Message, MessageType} from "../types/messages";

const VERBOSE_LOGGING = false && process.env.NODE_ENV === 'development';
const CONTAINER_ID = 'message-list-' + Math.random().toString(36).substr(2, 9);

const MessageListContainer = styled.div`
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    scroll-behavior: smooth;
    background: ${({theme}) => `linear-gradient(${theme.colors.background}, ${theme.colors.surface})`};

    &::-webkit-scrollbar {
        width: 10px;
    }

    &::-webkit-scrollbar-track {
        background: ${({theme}) => theme.colors.surface};
        border-radius: 4px;
        box-shadow: inset 0 0 6px rgba(0, 0, 0, 0.1);
    }

    &::-webkit-scrollbar-thumb {
        background: ${({theme}) => theme.colors.primary};
        border-radius: 4px;
        border: 2px solid ${({theme}) => theme.colors.surface};

        &:hover {
            background: ${({theme}) => theme.colors.primaryDark};
        }
    }
`;

const MessageContent = styled.div`
    /* Theme variables for consistent styling */
    color: var(--theme-text);
    background: var(--theme-bg);

    pre[class*="language-"],
    code[class*="language-"] {
        background: var(--theme-surface);
        color: var(--theme-text);
        font-family: var(--theme-code-font);
    }

    .href-link, .play-button, .regen-button, .cancel-button, .text-submit-button {
        cursor: pointer;
        user-select: none;
        display: inline-block;
        padding: 2px 8px;
        margin: 2px;
        border-radius: 4px;
        background-color: var(--theme-surface);
        color: var(--theme-text);
        transition: all var(--transition-duration) var(--transition-timing),
        transform 0.2s ease-in-out;

        &:hover {
            opacity: 0.8;
            background-color: var(--theme-primary);
            color: var(--theme-bg);
            transform: translateY(-1px);
        }
    }

    .referenced-message {
        cursor: pointer;
        padding: 4px;
        margin: 4px 0;
        border-left: 3px solid ${({theme}) => theme.colors.border};
        transition: all 0.3s ease;

        &.expanded {
            background-color: ${({theme}) => theme.colors.surface};
        }
    }

    pre[class*="language-"] {
        background: ${({theme}) => theme.colors.surface};
        margin: 1em 0;
        padding: 1em;
        border-radius: ${({theme}) => theme.sizing.borderRadius.md};
        transition: all var(--transition-duration) var(--transition-timing);
        box-shadow: ${({theme}) => theme.shadows.medium};
    }

    code[class*="language-"] {
        color: ${({theme}) => theme.colors.text.primary};
        text-shadow: none;
        transition: all 0.3s ease;
        font-family: ${({theme}) => theme.typography.console.fontFamily};
    }

    :not(pre) > code {
        background: ${({theme}) => theme.colors.surface};
        color: ${({theme}) => theme.colors.text.primary};
        padding: 0.2em 0.4em;
        border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
        font-size: 0.9em;
        transition: all 0.3s ease;
    }
`;
/**
 * Extracts message ID and action from clicked elements
 * Supports both data attributes and class-based detection
 */

const extractMessageAction = (target: HTMLElement): { messageId: string | undefined, action: string | undefined } => {
    const messageId = target.getAttribute('data-message-id') ??
        target.getAttribute('data-id') ??
        undefined;
    let action = target.getAttribute('data-message-action') ??
        target.getAttribute('data-action') ??
        undefined;
    if (!action) {
        if (target.classList.contains('href-link')) action = 'link';
        else if (target.classList.contains('play-button')) action = 'run';
        else if (target.classList.contains('regen-button')) action = 'regen';
        else if (target.classList.contains('cancel-button')) action = 'stop';
        else if (target.classList.contains('text-submit-button')) action = 'text-submit';
    }
    return {messageId, action};
};

const MessageItem = styled.div<{ type: MessageType }>`
    padding: 1rem;
    border-radius: 12px;
    align-self: ${({type}) => type === 'user' ? 'flex-end' : 'flex-start'};
    max-width: 80%;
    box-shadow: ${({theme}) => `${theme.shadows.medium}, 0 8px 16px rgba(0,0,0,0.1)`};
    transition: transform 0.2s ease;
    position: relative;
    overflow: visible;

    background-color: ${({type}) => {
        switch (type) {
            case 'user':
                return ({theme}) => `linear-gradient(135deg, ${theme.colors.primary}, ${theme.colors.primaryDark})`;
            case 'system':
                return ({theme}) => `linear-gradient(135deg, ${theme.colors.secondary}, ${theme.colors.info})`;
            case 'error':
                return ({theme}) => `linear-gradient(135deg, ${theme.colors.error}, ${theme.colors.warning})`;
            case 'loading':
                return ({theme}) => theme.colors.surface;
            case 'assistant':
                return ({theme}) => theme.colors.surface;
            case 'reference':
                return ({theme}) => theme.colors.surface;
            default:
                return ({theme}) => theme.colors.surface;
        }
    }};
    color: ${({type, theme}) =>
            type === 'user' || type === 'system' || type === 'error'
                    ? '#fff'
                    : theme.colors.text.primary};

    &:hover {
        transform: translateY(-3px);
        box-shadow: ${({theme}) => `${theme.shadows.large}, 0 12px 24px rgba(0,0,0,0.15)`};
    }

    &:after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(rgba(255, 255, 255, 0.1), transparent);
        pointer-events: none;
    }
`;

const handleClick = (e: React.MouseEvent) => {
    const target = e.target as HTMLElement;
    const {messageId, action} = extractMessageAction(target);
    if (messageId && action) {
        console.debug('Message action clicked', {messageId, action});
        e.preventDefault();
        e.stopPropagation();
        handleMessageAction(messageId, action);
    }
};

export const handleMessageAction = (messageId: string, action: string) => {
    if (VERBOSE_LOGGING) {
        console.debug(`${'Processing message action'}`, {messageId, action, containerId: CONTAINER_ID});
    }

    if (action === 'text-submit') {
        const input = document.querySelector(`.reply-input[data-message-id="${messageId}"]`) as HTMLTextAreaElement;
        if (input) {
            const text = input.value;
            const escapedText = encodeURIComponent(text);
            const message = `!${messageId},userTxt,${escapedText}`;
            WebSocketService.send(message);
            if (VERBOSE_LOGGING) {
                console.debug(`${'Text submitted'}`, {
                    containerId: CONTAINER_ID,
                    messageId,
                    previewText: text.substring(0, 50) + (text.length > 50 ? '...' : '')
                });
            }
            input.value = '';
        }
        return;
    }
    /**
     * Recursively expands referenced messages within content
     * Prevents infinite loops using processedRefs Set
     */
    if (action === 'link') {
        console.debug('Processing link click', {messageId});
        WebSocketService.send(`!${messageId},link`);
        return;
    }
    if (action === 'run') {
        console.debug('Processing run action', {messageId});
        WebSocketService.send(`!${messageId},run`);
        return;
    }
    if (action === 'regen') {
        console.debug('Processing regenerate action', {messageId});
        WebSocketService.send(`!${messageId},regen`);
        return;
    }
    if (action === 'stop') {
        console.debug('Processing stop action', {messageId});
        WebSocketService.send(`!${messageId},stop`);
        return;
    }
    console.debug('Processing generic action', {messageId, action});
    WebSocketService.send(`!${messageId},${action}`);
};

interface MessageListProps {
    messages?: Message[];
}

export const expandMessageReferences = (content: string, messages: Message[]): string => {
    if (!content) return '';
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = content;
    const processedRefs = new Set<string>();

    const processNode = (node: HTMLElement) => {
        const messageID = node.getAttribute("message-id");
        if (messageID && !processedRefs.has(messageID)) {
            if (messageID?.startsWith('z')) {
                processedRefs.add(messageID); // Mark this reference as processed
                const referencedMessage = messages.find(m => m.id === messageID);
                if (referencedMessage) {
                    node.innerHTML = expandMessageReferences(referencedMessage.content, messages);
                } else {
                    console.debug('Referenced message not found', {id: messageID});
                }
            }
        }
        Array.from(node.children).forEach(child => {
            if (child instanceof HTMLElement) {
                processNode(child);
            }
        });
    };
    processNode(tempDiv);
    return tempDiv.innerHTML;
};

const MessageList: React.FC<MessageListProps> = ({messages: propMessages}) => {
    React.useEffect(() => {
        if (VERBOSE_LOGGING) {
            console.debug(`${'Component lifecycle'}`, {
                event: 'mounted',
                containerId: CONTAINER_ID,
                timestamp: new Date().toISOString()
            });
        }
        return () => {
            if (VERBOSE_LOGGING) {
                console.debug(`${'Component lifecycle'}`, {
                    event: 'unmounted',
                    containerId: CONTAINER_ID
                });
            }
        };
    }, []);

    const storeMessages = useSelector((state: RootState) => state.messages.messages);
    const messages = Array.isArray(propMessages) ? propMessages :
        Array.isArray(storeMessages) ? storeMessages : [];
    const messageListRef = useRef<HTMLDivElement>(null);
    const referencesVersions = React.useMemo(() => {
        const versions: Record<string, number> = {};
        messages.forEach(msg => {
            if (msg.id?.startsWith('z')) {
                versions[msg.id] = msg.version || 0;
            }
        });
        return versions;
    }, [messages]);

    const finalMessages = React.useMemo(() => messages
            .filter((message) => message.id && !message.id.startsWith("z"))
            .filter((message) => message.content?.length > 0).map((message) => (
                {
                    ...message,
                    content: expandMessageReferences(message.content, messages)
                }
            )),
        [messages, referencesVersions]); // Add referencesVersions as dependency

    useEffect(() => {
        if (messageListRef.current) {
            const codeBlocks = messageListRef.current.querySelectorAll('pre code');
            if (VERBOSE_LOGGING) {
                console.debug(`Syntax highlighting`, {
                    blockCount: codeBlocks.length,
                    containerId: CONTAINER_ID
                });
            }
            codeBlocks.forEach(block => {
                Prism.highlightElement(block);
            });
        }
    }, [messages]);

    useTheme();
    console.log('MessageList', 'Rendering component', {hasPropMessages: !!propMessages});

    React.useEffect(() => {
        try {
            if (VERBOSE_LOGGING) {
                console.debug(`${'Tab state update'}`, {
                    messageCount: finalMessages.length,
                    containerId: CONTAINER_ID
                });
            }
            updateTabs();
        } catch (error) {
            console.error(`[MessageList ${CONTAINER_ID}] ${'Failed to update tabs'}`, error);
            resetTabState();
        }
    }, [finalMessages]);

    return <MessageListContainer ref={messageListRef}>
        {finalMessages.map((message) => {
            console.debug('MessageList - Rendering message', {
                id: message.id,
                type: message.type,
                timestamp: message.timestamp,
                contentLength: message.content?.length || 0
            });
            return <MessageItem
                key={message.id} // Changed key to use only message.id
                type={message.type}
            >
                {<MessageContent
                    className="message-body"
                    onClick={handleClick}
                    dangerouslySetInnerHTML={{
                        __html: message.content
                    }}
                />}
            </MessageItem>;
        })}
    </MessageListContainer>;
};


export default MessageList;