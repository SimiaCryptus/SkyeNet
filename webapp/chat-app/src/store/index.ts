import { configureStore } from '@reduxjs/toolkit';
import configReducer from './slices/configSlice';
import messageReducer from './slices/messageSlice';
import uiReducer from './slices/uiSlice';
import userReducer from './slices/userSlice';

export const store = configureStore({
  reducer: {
    ui: uiReducer,
    config: configReducer,
    messages: messageReducer,
    user: userReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
