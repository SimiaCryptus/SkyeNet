import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {Message} from '../../types';

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
      console.log('Adding message:', action.payload);
      state.messages.push(action.payload);
      console.log('Updated messages state:', state.messages);
    },
    updateMessage: (state: MessageState, action: PayloadAction<{ id: string; updates: Partial<Message> }>) => {
      const { id, updates } = action.payload;
      console.log('Updating message:', id, 'with updates:', updates);
      const messageIndex = state.messages.findIndex((msg: Message) => msg.id === id);
      if (messageIndex !== -1) {
        state.messages[messageIndex] = { ...state.messages[messageIndex], ...updates };
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