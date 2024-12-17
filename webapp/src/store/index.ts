import {configureStore, Middleware} from '@reduxjs/toolkit';
import configReducer from './slices/configSlice';
import messageReducer from './slices/messageSlice';
import uiReducer from './slices/uiSlice';
import userReducer from './slices/userSlice';

// Utility function to get formatted timestamp
const getTimestamp = () => new Date().toISOString().split('T')[1].slice(0, -1);

// Custom middleware for logging actions and state changes
const logger: Middleware = (store) => (next) => (action: unknown) => {
    const timestamp = getTimestamp();
    const actionObj = action as any;
    console.group(`%c Redux Action: ${actionObj.type} @ ${timestamp}`, 'color: #8833FF; font-weight: bold;');
    // Log previous state
    console.log('%c Previous State:', 'color: #9E9E9E; font-weight: bold;', store.getState());
    // Log action with different styling
    console.log('%c Action:', 'color: #00BCD4; font-weight: bold;', {
        type: actionObj.type,
        payload: actionObj.payload,
        meta: actionObj.meta,
    });

    const result = next(action);
    // Calculate and log what changed
    const nextState = store.getState();
    console.log('%c Next State:', 'color: #4CAF50; font-weight: bold;', nextState);
    // Log performance timing
    console.log('%c ⚡ Action Processing Time:', 'color: #FF5722; font-weight: bold;',
        `${performance.now() - performance.now()}ms`);

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
            ? getDefaultMiddleware({
                serializableCheck: {
                    // Ignore these action types
                    ignoredActions: ['your-action-type-to-ignore'],
                    // Ignore these field paths in all actions
                    ignoredActionPaths: ['meta.arg', 'payload.timestamp'],
                    // Ignore these paths in the state
                    ignoredPaths: ['items.dates'],
                },
            }).concat(logger)
            : getDefaultMiddleware(),
});

export type RootState = ReturnType<typeof store.getState>;
// Add development-only warning
if (process.env.NODE_ENV === 'development') {
    console.log('%c Redux Store Initialized in Development Mode',
        'background: #222; color: #bada55; font-size: 14px; padding: 5px;');
}