import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { Message } from '../../types';

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

const messageSlice = createSlice({
  name: 'messages',
  initialState,
  reducers: {
    addMessage: (state: MessageState, action: PayloadAction<Message>) => {
      state.messages.push(action.payload);
    },
    updateMessage: (state: MessageState, action: PayloadAction<{ id: string; updates: Partial<Message> }>) => {
      const { id, updates } = action.payload;
      const messageIndex = state.messages.findIndex((msg: Message) => msg.id === id);
      if (messageIndex !== -1) {
        state.messages[messageIndex] = { ...state.messages[messageIndex], ...updates };
      }
    },
    deleteMessage: (state: MessageState, action: PayloadAction<string>) => {
      state.messages = state.messages.filter((msg: Message) => msg.id !== action.payload);
    },
    addToPendingMessages: (state: MessageState, action: PayloadAction<Message>) => {
      state.pendingMessages.push(action.payload);
    },
    removePendingMessage: (state: MessageState, action: PayloadAction<string>) => {
      state.pendingMessages = state.pendingMessages.filter((msg: Message) => msg.id !== action.payload);
    },
    addToMessageQueue: (state, action: PayloadAction<Message>) => {
      state.messageQueue.push(action.payload);
    },
    clearMessageQueue: (state: MessageState) => {
      state.messageQueue = [];
    },
    setProcessing: (state: MessageState, action: PayloadAction<boolean>) => {
      state.isProcessing = action.payload;
    },
    clearMessages: (state: MessageState) => {
      state.messages = [];
      state.pendingMessages = [];
      state.messageQueue = [];
      state.isProcessing = false;
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