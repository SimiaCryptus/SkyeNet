import React, {useCallback} from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {useTheme} from '../hooks/useTheme';
import {RootState} from '../store';
import {logger} from '../utils/logger';
import {Message} from '../types';
import {getAllTabStates, resetTabState, saveTabState, setActiveTabState, updateTabs} from '../utils/tabHandling';
import WebSocketService from "../services/websocket";
import Prism from 'prismjs';

export const expandMessageReferences = (content: string, messages: Message[]): string => {
    if (!content) return '';
    // Create a temporary div to parse HTML content
    const tempDiv = document.createElement('div');
    tempDiv.innerHTML = content;
    // Helper to highlight code blocks in content
    const highlightCodeBlocks = (element: HTMLElement) => {
        const codeBlocks = element.querySelectorAll('pre code');
        codeBlocks.forEach(block => {
            if (block instanceof HTMLElement) {
                Prism.highlightElement(block);
            }
        });
    };
    // Process all elements with IDs that match message references
    const processNode = (node: HTMLElement) => {
        if (node.id && node.id.startsWith('z')) {
            const referencedMessage = messages.find(m => m.id === node.id);
            if (referencedMessage) {
                // logger.debug('Expanding referenced message', {id: node.id, contentLength: referencedMessage.content.length});
                node.innerHTML = expandMessageReferences(referencedMessage.content, messages);
                highlightCodeBlocks(node);
            } else {
                // logger.debug('Referenced message not found', {id: node.id});
            }
        }
        // Recursively process child elements
        Array.from(node.children).forEach(child => {
            if (child instanceof HTMLElement) {
                processNode(child);
            }
        });
    };
    // logger.debug('Expanding message references', {content});
    processNode(tempDiv);
    highlightCodeBlocks(tempDiv);
    return tempDiv.innerHTML;
};

const MessageListContainer = styled.div`
    flex: 1;
    overflow-y: auto;
    padding: 1rem;
    display: flex;
    flex-direction: column;
    gap: 1rem;
    max-height: 85vh;
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

    /* Style code blocks according to theme */

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

    /* Style inline code differently from code blocks */

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

export const handleMessageAction = (messageId: string, action: string) => {
    logger.debug('Processing message action', {messageId, action});

    // Handle text submit actions specially
    if (action === 'text-submit') {
        const input = document.querySelector(`.reply-input[data-message-id="${messageId}"]`) as HTMLTextAreaElement;
        if (input) {
            const text = input.value;
            const escapedText = encodeURIComponent(text);
            const message = `!${messageId},userTxt,${escapedText}`;
            WebSocketService.send(message);
            logger.debug('Sent text submit message', {messageId, text: text.substring(0, 100)});
            input.value = '';
        }
        return;
    }
    // Handle link clicks
    if (action === 'link') {
        logger.debug('Processing link click', {messageId});
        WebSocketService.send(`!${messageId},link`);
        return;
    }
    // Handle run/play button clicks
    if (action === 'run') {
        logger.debug('Processing run action', {messageId});
        WebSocketService.send(`!${messageId},run`);
        return;
    }
    // Handle regenerate button clicks
    if (action === 'regen') {
        logger.debug('Processing regenerate action', {messageId});
        WebSocketService.send(`!${messageId},regen`);
        return;
    }
    // Handle cancel button clicks
    if (action === 'stop') {
        logger.debug('Processing stop action', {messageId});
        WebSocketService.send(`!${messageId},stop`);
        return;
    }
    // Handle all other actions
    logger.debug('Processing generic action', {messageId, action});
    WebSocketService.send(`!${messageId},${action}`);
};

interface MessageListProps {
    messages?: Message[];
}

const MessageList: React.FC<MessageListProps> = ({messages: propMessages}) => {
    const storeMessages = useSelector((state: RootState) => state.messages.messages);
    const messages = Array.isArray(propMessages) ? propMessages :
        Array.isArray(storeMessages) ? storeMessages : [];

    // Memoize message processing
    const memoizedProcessMessageContent = React.useMemo(() => {
        return (content: string) => {
            if (!content) return '';
            return expandMessageReferences(content, messages);
        };
    }, [messages]);

    // Optimize message filtering
    const filteredMessages = React.useMemo(() => {
        return messages
            .filter((message) => message.id && !message.id.startsWith("z"))
            .filter((message) => message.content?.length > 0);
    }, [messages]);
    const theme = useTheme();
    logger.component('MessageList', 'Rendering component', {hasPropMessages: !!propMessages});
    // Store tab states on mount
    React.useEffect(() => {
        logger.debug('MessageList - Initial tab state setup');
        const containers = document.querySelectorAll('.tabs-container');
        containers.forEach(container => {
            if (container instanceof HTMLElement) {
                const activeTab = container.querySelector('.tab-button.active');
                if (activeTab instanceof HTMLElement) {
                    const forTab = activeTab.getAttribute('data-for-tab');
                    if (forTab && container.id) {
                        logger.debug('MessageList - Saving initial tab state:', {
                            containerId: container.id,
                            activeTab: forTab
                        });
                        saveTabState(container.id, forTab);
                        // Also store in active tab states
                        setActiveTabState(container.id, forTab);
                    }
                }
            }
        });
    }, []);
    // Store current tab states before update
    const preserveTabStates = useCallback(() => {
        const containers = document.querySelectorAll('.tabs-container');
        containers.forEach(container => {
            const activeTab = container.querySelector('.tab-button.active');
            if (activeTab instanceof HTMLElement) {
                const forTab = activeTab.getAttribute('data-for-tab');
                if (forTab && container.id) {
                    saveTabState(container.id, forTab);
                }
            }
        });
    }, []);


    // Log when component is mounted/unmounted
    React.useEffect(() => {
        logger.component('MessageList', 'Component mounted', {timestamp: new Date().toISOString()});
        return () => {
            logger.component('MessageList', 'Component unmounted', {timestamp: new Date().toISOString()});
        };
    }, []);

    React.useEffect(() => {
        logger.debug('MessageList - Messages updated', {
            messageCount: messages.length,
            messageIds: messages.map(m => m.id),
            source: propMessages ? 'props' : 'store'
        });
        // Log current tab states before preservation
        const currentStates = getAllTabStates();
        logger.debug('MessageList - Current tab states before update:', {
            states: Array.from(currentStates.entries())
        });

        // Preserve current tab states
        preserveTabStates();


        // Process tabs after messages update
        requestAnimationFrame(() => {
            try {
                logger.debug('MessageList - Updating tabs after message change');
                updateTabs();
            } catch (error) {
                logger.error('Error processing tabs:', error);
                // Reset tab state on error
                resetTabState();
            }
        });
    }, [messages]);

    return (
        <MessageListContainer>
            {filteredMessages
                .map((message) => {
                    logger.debug('MessageList - Rendering message', {
                        id: message.id,
                        type: message.type,
                        timestamp: message.timestamp,
                        contentLength: message.content?.length || 0
                    });
                    return (
                        <MessageItem
                            key={`${message.id}-${message.timestamp}`}
                            type={message.type}
                        >
                            <MessageContent
                                className="message-body"
                                onClick={handleClick}
                                dangerouslySetInnerHTML={{
                                    __html: memoizedProcessMessageContent(message.content)
                                }}
                            />
                        </MessageItem>
                    );
                })}
        </MessageListContainer>
    );
};


export default MessageList;