import {configureStore} from '@reduxjs/toolkit';
import configReducer from './slices/configSlice';
import messageReducer from './slices/messageSlice';
import uiReducer from './slices/uiSlice';
import userReducer from './slices/userSlice';
import {Middleware} from 'redux';
// Custom middleware for logging actions and state changes
const logger: Middleware = (store) => (next) => (action) => {
  console.group(`Redux Action: ${action.type}`);
  console.log('Previous State:', store.getState());
  console.log('Action:', action);
  const result = next(action);
  console.log('Next State:', store.getState());
  console.groupEnd();
  return result;
};

export const store = configureStore({
  reducer: {
    ui: uiReducer,
    config: configReducer,
    messages: messageReducer,
    user: userReducer,
  },
  middleware: (getDefaultMiddleware) =>
      process.env.NODE_ENV === 'development'
          ? getDefaultMiddleware().concat(logger)
          : getDefaultMiddleware(),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;