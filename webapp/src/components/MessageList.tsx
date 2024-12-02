import React, {useEffect, useRef} from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {useTheme} from '../hooks/useTheme';
import {RootState} from '../store';

import {Message} from '../types';
import {resetTabState, saveTabState, updateTabs} from '../utils/tabHandling';
import WebSocketService from "../services/websocket";
import Prism from 'prismjs';

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
    /* Add theme-specific CSS variables */
    --theme-bg: ${({theme}) => theme.colors.background};
    --theme-text: ${({theme}) => theme.colors.text.primary};
    --theme-surface: ${({theme}) => theme.colors.surface};
    --theme-border: ${({theme}) => theme.colors.border};
    --theme-primary: ${({theme}) => theme.colors.primary};
    --theme-code-font: ${({theme}) => theme.typography.console.fontFamily};
    /* Apply theme variables to content */
    color: var(--theme-text);
    background: var(--theme-bg);
    /* Style code blocks with theme variables */

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

const MessageItem = styled.div<{ type: 'user' | 'system' | 'response' }>`
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
            default:
                return ({theme}) => theme.colors.surface;
        }
    }};
    color: ${({type, theme}) =>
            type === 'user' || type === 'system'
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
    console.debug('Processing message action', {messageId, action});

    // Handle text submit actions specially
    if (action === 'text-submit') {
        const input = document.querySelector(`.reply-input[data-message-id="${messageId}"]`) as HTMLTextAreaElement;
        if (input) {
            const text = input.value;
            const escapedText = encodeURIComponent(text);
            const message = `!${messageId},userTxt,${escapedText}`;
            WebSocketService.send(message);
            console.debug('Sent text submit message', {messageId, text: text.substring(0, 100)});
            input.value = '';
        }
        return;
    }
    // Handle link clicks
    if (action === 'link') {
        console.debug('Processing link click', {messageId});
        WebSocketService.send(`!${messageId},link`);
        return;
    }
    // Handle run/play button clicks
    if (action === 'run') {
        console.debug('Processing run action', {messageId});
        WebSocketService.send(`!${messageId},run`);
        return;
    }
    // Handle regenerate button clicks
    if (action === 'regen') {
        console.debug('Processing regenerate action', {messageId});
        WebSocketService.send(`!${messageId},regen`);
        return;
    }
    // Handle cancel button clicks
    if (action === 'stop') {
        console.debug('Processing stop action', {messageId});
        WebSocketService.send(`!${messageId},stop`);
        return;
    }
    // Handle all other actions
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
    const processedRefs = new Set<string>(); // Track processed references to prevent infinite loops

    const processNode = (node: HTMLElement) => {
        const messageID = node.getAttribute("message-id");
        if (messageID && !processedRefs.has(messageID)) {
            if (messageID?.startsWith('z')) {
                processedRefs.add(messageID); // Mark this reference as processed
                const referencedMessage = messages.find(m => m.id === messageID);
                if (referencedMessage) {
                    //logger.debug('Expanding referenced message', {id: messageID, contentLength: referencedMessage.content.length});
                    node.innerHTML = expandMessageReferences(referencedMessage.content, messages);
                } else {
                    console.debug('Referenced message not found', {id: messageID});
                    node.innerHTML = `<em>Loading reference ${messageID}...</em>`;
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
        console.log('MessageList', 'Component mounted', {timestamp: new Date().toISOString()});
        return () => {
            console.log('MessageList', 'Component unmounted', {timestamp: new Date().toISOString()});
        };
    }, []);

    const storeMessages = useSelector((state: RootState) => state.messages.messages);
    const messages = Array.isArray(propMessages) ? propMessages :
        Array.isArray(storeMessages) ? storeMessages : [];
    const messageListRef = useRef<HTMLDivElement>(null);
    // Track processed reference versions to detect changes
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

    // Effect to handle syntax highlighting after render
    useEffect(() => {
        if (messageListRef.current) {
            const codeBlocks = messageListRef.current.querySelectorAll('pre code');
            console.debug('Highlighting code blocks:', {count: codeBlocks.length});
            codeBlocks.forEach(block => {
                Prism.highlightElement(block);
            });
        }
    }, [messages]); // Re-run when messages or references change
    useTheme();
    console.log('MessageList', 'Rendering component', {hasPropMessages: !!propMessages});

    React.useEffect(() => {
        document.querySelectorAll('.tabs-container').forEach(container => {
            const activeTab = container.querySelector('.tab-button.active');
            if (activeTab instanceof HTMLElement) {
                const forTab = activeTab.getAttribute('data-for-tab');
                if (forTab && container.id) {
                    saveTabState(container.id, forTab);
                }
            }
        });
        try {
            console.debug('MessageList - Updating tabs after message change');
            updateTabs();
            // Prism.highlightAll();
        } catch (error) {
            console.error('Error processing tabs:', error);
            // Reset tab state on error
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