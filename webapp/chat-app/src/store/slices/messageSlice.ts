import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {Message} from '../../types';
import DOMPurify from 'dompurify';
import {RootState} from '../index';

const LOG_PREFIX = '[MessageSlice]';

interface MessageState {
    messages: Message[];
    pendingMessages: Message[];
    messageQueue: Message[];
    isProcessing: boolean;
}

const initialState: MessageState = {
    messages: [],
    pendingMessages: [],
    messageQueue: [],
    isProcessing: false,
};
// Add type-safe selector
// Ensure selector always returns an array
export const selectMessages = (state: RootState) => {
    const messages = state.messages?.messages;
    return Array.isArray(messages) ? messages : [];
};
// Add selector for messages
const sanitizeHtmlContent = (content: string): string => {
    console.debug(`${LOG_PREFIX} Sanitizing HTML content`);
    return DOMPurify.sanitize(content, {
        ALLOWED_TAGS: ['div', 'span', 'p', 'br', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'code', 'pre', 'table', 'tr', 'td', 'th', 'thead', 'tbody', 'button', 'input', 'label', 'select', 'option', 'textarea'],
        ALLOWED_ATTR: ['class', 'href', 'target']
    });
};

const messageSlice = createSlice({
    name: 'messages',
    initialState,
    reducers: {
        addMessage: (state: MessageState, action: PayloadAction<Message>) => {
            // Only log message ID and type to reduce noise
            console.debug(`${LOG_PREFIX} Adding message:`, {
                id: action.payload.id,
                type: action.payload.type,
                isHtml: action.payload.isHtml
            });
            // Only sanitize if not already sanitized
            if (action.payload.isHtml && action.payload.rawHtml && !action.payload.sanitized) {
                action.payload.content = action.payload.rawHtml;
                // action.payload.content = sanitizeHtmlContent(action.payload.rawHtml);
                action.payload.sanitized = true;
                console.debug(`${LOG_PREFIX} HTML content sanitized for message ${action.payload.id}`);
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