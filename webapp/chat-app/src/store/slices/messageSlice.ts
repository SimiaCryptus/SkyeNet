import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {Message} from '../../types';
import DOMPurify from 'dompurify';

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
const sanitizeHtmlContent = (content: string): string => {
    console.debug('Sanitizing HTML content');
    return DOMPurify.sanitize(content, {
        ALLOWED_TAGS: ['div', 'span', 'p', 'br', 'b', 'i', 'em', 'strong', 'a', 'ul', 'ol', 'li', 'code', 'pre', 'table', 'tr', 'td', 'th', 'thead', 'tbody'],
        ALLOWED_ATTR: ['class', 'href', 'target']
    });
};

const messageSlice = createSlice({
    name: 'messages',
    initialState,
    reducers: {
        addMessage: (state: MessageState, action: PayloadAction<Message>) => {
            // Only log message ID and type to reduce noise
            console.debug('Adding message:', {
                id: action.payload.id,
                type: action.payload.type,
                isHtml: action.payload.isHtml
            });
            // Only sanitize if not already sanitized
            if (action.payload.isHtml && action.payload.rawHtml && !action.payload.sanitized) {
                action.payload.content = sanitizeHtmlContent(action.payload.rawHtml);
                action.payload.sanitized = true;
                console.debug('HTML content sanitized');
            }
            state.messages.push(action.payload);
            console.debug('Messages updated, count:', state.messages.length);
        },
        updateMessage: (state: MessageState, action: PayloadAction<{ id: string; updates: Partial<Message> }>) => {
            const {id, updates} = action.payload;
            console.log('Updating message:', id, 'with updates:', updates);
            const messageIndex = state.messages.findIndex((msg: Message) => msg.id === id);
            if (messageIndex !== -1) {
                state.messages[messageIndex] = {...state.messages[messageIndex], ...updates};
                console.log('Message updated successfully');
            } else {
                console.warn('Message not found for update:', id);
            }
        },
        deleteMessage: (state: MessageState, action: PayloadAction<string>) => {
            console.log('Deleting message:', action.payload);
            state.messages = state.messages.filter((msg: Message) => msg.id !== action.payload);
            console.log('Updated messages after deletion:', state.messages);
        },
        addToPendingMessages: (state: MessageState, action: PayloadAction<Message>) => {
            console.log('Adding pending message:', action.payload);
            state.pendingMessages.push(action.payload);
            console.log('Updated pending messages:', state.pendingMessages);
        },
        removePendingMessage: (state: MessageState, action: PayloadAction<string>) => {
            console.log('Removing pending message:', action.payload);
            state.pendingMessages = state.pendingMessages.filter((msg: Message) => msg.id !== action.payload);
            console.log('Updated pending messages:', state.pendingMessages);
        },
        addToMessageQueue: (state, action: PayloadAction<Message>) => {
            console.log('Adding message to queue:', action.payload);
            state.messageQueue.push(action.payload);
            console.log('Updated message queue:', state.messageQueue);
        },
        clearMessageQueue: (state: MessageState) => {
            console.log('Clearing message queue');
            state.messageQueue = [];
        },
        setProcessing: (state: MessageState, action: PayloadAction<boolean>) => {
            console.log('Setting processing state:', action.payload);
            state.isProcessing = action.payload;
        },
        clearMessages: (state: MessageState) => {
            console.log('Clearing all messages and states');
            state.messages = [];
            state.pendingMessages = [];
            state.messageQueue = [];
            state.isProcessing = false;
            console.log('All states cleared');
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