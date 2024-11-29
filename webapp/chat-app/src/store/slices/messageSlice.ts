import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {Message} from '../../types';
import DOMPurify from 'dompurify';
import {resetTabState, updateTabs} from '../../utils/tabHandling';

const LOG_PREFIX = '[MessageSlice]';

interface MessageState {
    messages: Message[];
    pendingMessages: Message[];
    messageQueue: Message[];
    isProcessing: boolean;
    referenceMessages: Record<string, Message>;
    messageVersions: Record<string, string>; // Track message versions
}

const initialState: MessageState = {
    messages: [],
    pendingMessages: [],
    messageQueue: [],
    isProcessing: false,
    messageVersions: {},
    referenceMessages: {},
};

const sanitizeHtmlContent = (content: string): string => {
    console.debug(`${LOG_PREFIX} Sanitizing HTML content`);
    return DOMPurify.sanitize(content, {
        ALLOWED_TAGS: ['div', 'span', 'p', 'br', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'code', 'pre', 'table', 'tr', 'td', 'th', 'thead', 'tbody',
            'button', 'input', 'label', 'select', 'option', 'textarea', 'code', 'pre', 'div', 'section'],
        ALLOWED_ATTR: ['class', 'href', 'target', 'data-tab', 'data-for-tab', 'style', 'type', 'value', 'id', 'name',
            'data-message-id', 'data-id', 'data-message-action', 'data-action', 'data-ref-id', 'data-version'],
    });
};

const messageSlice = createSlice({
    name: 'messages',
    initialState,
    reducers: {
        addMessage: (state: MessageState, action: PayloadAction<Message>) => {
            const messageId = action.payload.id;
            const messageVersion = action.payload.version;
            const existingVersion = state.messageVersions[messageId];
            // Skip processing if message is older or duplicate
            if (existingVersion && existingVersion >= messageVersion) {
                console.debug(`${LOG_PREFIX} Ignoring older/duplicate message version:`, {
                    id: messageId,
                    existing: existingVersion,
                    received: messageVersion
                });
                return;
            }
            // Update version tracking
            state.messageVersions[messageId] = messageVersion;

            // Store reference messages separately
            if (messageId.startsWith('z')) {
                state.referenceMessages[messageId] = action.payload;
            }

            console.debug(`${LOG_PREFIX} Adding message:`, {
                id: messageId,
                version: messageVersion,
                type: action.payload.type,
                isHtml: action.payload.isHtml,
                // payload: action.payload,
            });
            state.messageVersions[messageId] = messageVersion;
            if (existingVersion) {
                // Update the message in place instead of removing and re-adding
                const existingIndex = state.messages.findIndex(msg => msg.id === messageId);
                if (existingIndex !== -1) {
                    if (action.payload.isHtml && action.payload.rawHtml && !action.payload.sanitized) {
                        action.payload.content = sanitizeHtmlContent(action.payload.rawHtml);
                        action.payload.sanitized = true;
                        console.debug(`${LOG_PREFIX} HTML content sanitized for message ${action.payload.id}`);
                        requestAnimationFrame(() => {
                            updateTabs();
                        });
                    }
                    state.messages[existingIndex] = action.payload;
                    console.debug(`${LOG_PREFIX} Updated existing message at index ${existingIndex}`);
                    return;
                }
            }
            resetTabState();

            if (action.payload.isHtml && action.payload.rawHtml && !action.payload.sanitized) {
                action.payload.content = sanitizeHtmlContent(action.payload.rawHtml);
                action.payload.sanitized = true;
                console.debug(`${LOG_PREFIX} HTML content sanitized for message ${action.payload.id}`);
                // Use requestAnimationFrame for smoother updates
                requestAnimationFrame(() => {
                    updateTabs();
                });
            }
            state.messages.push(action.payload);
            console.debug(`${LOG_PREFIX} Messages updated, total count: ${state.messages.length}`);
        },
        updateMessage: (state: MessageState, action: PayloadAction<{ id: string; updates: Partial<Message> }>) => {
            const {id, updates} = action.payload;
            console.debug(`${LOG_PREFIX} Updating message ${id}:`, updates);
            const messageIndex = state.messages.findIndex((msg: Message) => msg.id === id);
            if (messageIndex !== -1) {
                state.messages[messageIndex] = {...state.messages[messageIndex], ...updates};
                console.debug(`${LOG_PREFIX} Message ${id} updated successfully`);
            } else {
                console.warn(`${LOG_PREFIX} Message not found for update: ${id}`);
            }
        },
        deleteMessage: (state: MessageState, action: PayloadAction<string>) => {
            console.debug(`${LOG_PREFIX} Deleting message: ${action.payload}`);
            state.messages = state.messages.filter((msg: Message) => msg.id !== action.payload);
            console.debug(`${LOG_PREFIX} Messages updated after deletion, remaining: ${state.messages.length}`);
        },
        addToPendingMessages: (state: MessageState, action: PayloadAction<Message>) => {
            console.debug(`${LOG_PREFIX} Adding pending message:`, {
                id: action.payload.id,
                type: action.payload.type
            });
            state.pendingMessages.push(action.payload);
            console.debug(`${LOG_PREFIX} Pending messages count: ${state.pendingMessages.length}`);
        },
        removePendingMessage: (state: MessageState, action: PayloadAction<string>) => {
            console.debug(`${LOG_PREFIX} Removing pending message: ${action.payload}`);
            state.pendingMessages = state.pendingMessages.filter((msg: Message) => msg.id !== action.payload);
            console.debug(`${LOG_PREFIX} Pending messages count: ${state.pendingMessages.length}`);
        },
        addToMessageQueue: (state, action: PayloadAction<Message>) => {
            console.debug(`${LOG_PREFIX} Adding message to queue:`, {
                id: action.payload.id,
                type: action.payload.type
            });
            state.messageQueue.push(action.payload);
            console.debug(`${LOG_PREFIX} Message queue size: ${state.messageQueue.length}`);
        },
        clearMessageQueue: (state: MessageState) => {
            console.debug(`${LOG_PREFIX} Clearing message queue of ${state.messageQueue.length} messages`);
            state.messageQueue = [];
        },
        setProcessing: (state: MessageState, action: PayloadAction<boolean>) => {
            console.debug(`${LOG_PREFIX} Setting processing state to: ${action.payload}`);
            state.isProcessing = action.payload;
        },
        clearMessages: (state: MessageState) => {
            console.debug(`${LOG_PREFIX} Clearing all messages and states`, {
                messages: state.messages.length,
                pending: state.pendingMessages.length,
                queue: state.messageQueue.length
            });
            state.messages = [];
            state.pendingMessages = [];
            state.messageQueue = [];
            state.isProcessing = false;
            console.debug(`${LOG_PREFIX} All states cleared successfully`);
        },
    },
});

export const {
    addMessage,
    updateMessage,
    deleteMessage,
    addToPendingMessages,
    removePendingMessage,
    addToMessageQueue,
    clearMessageQueue,
    setProcessing,
    clearMessages,
} = messageSlice.actions;

export default messageSlice.reducer;