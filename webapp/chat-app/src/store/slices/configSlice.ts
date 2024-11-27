import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {AppConfig, ThemeName} from '../../types';
// Helper function to validate theme name
const isValidTheme = (theme: string | null): theme is ThemeName => {
    return theme === 'main' || theme === 'night' || theme === 'forest' ||
        theme === 'pony' || theme === 'alien';
};
// Load theme from localStorage with type safety
const loadSavedTheme = (): ThemeName => {
    const savedTheme = localStorage.getItem('theme');
    return isValidTheme(savedTheme) ? savedTheme : 'main';
};
// Load websocket config from localStorage or use defaults
const loadWebSocketConfig = () => {
    try {
        const savedConfig = localStorage.getItem('websocketConfig');
        if (savedConfig) {
            console.log('[ConfigSlice] Loading saved WebSocket config:', {
                source: 'localStorage',
                config: JSON.parse(savedConfig)
            });
            return JSON.parse(savedConfig);
        }
    } catch (error) {
        console.error('[ConfigSlice] Failed to load WebSocket config:', {
            source: 'localStorage',
            error
        });
    }
    return {
        url: window.location.hostname,
        port: window.location.port,
        protocol: window.location.protocol === 'https:' ? 'wss:' : 'ws:',
        retryAttempts: 3,
        timeout: 5000
    };
};


const initialState: AppConfig = {
    singleInput: false,
    stickyInput: true,
    loadImages: true,
    showMenubar: true,
    applicationName: 'Chat App',
    websocket: loadWebSocketConfig(),
    logging: {
        enabled: true,
        level: 'info',
        maxEntries: 1000,
        persistLogs: false,
        console: {
            enabled: true,
            showTimestamp: true,
            showLevel: true,
            showSource: true,
            styles: {
                debug: {color: '#6c757d'},
                info: {color: '#17a2b8'},
                warn: {color: '#ffc107', bold: true},
                error: {color: '#dc3545', bold: true}
            }
        }
    },
    theme: {
        current: loadSavedTheme(),
        autoSwitch: false
    }
};

const configSlice = createSlice({
    name: 'config',
    initialState,
    reducers: {
        resetConfig: () => {
            console.log('[ConfigSlice] Resetting to initial state', {
                newState: initialState
            });
            return initialState;
        },
        setConnectionConfig: (state, action: PayloadAction<{
            retryAttempts: number;
            timeout: number;
        }>) => {
            console.log('[ConfigSlice] Updating connection config:', {
                previous: {
                    retryAttempts: state.websocket.retryAttempts,
                    timeout: state.websocket.timeout
                },
                new: action.payload
            });
            state.websocket.retryAttempts = action.payload.retryAttempts;
            state.websocket.timeout = action.payload.timeout;
        },
        setTheme: (state, action: PayloadAction<ThemeName>) => {
            console.log('[ConfigSlice] Setting theme:', {
                previous: state.theme.current,
                new: action.payload
            });
            state.theme.current = action.payload;
            localStorage.setItem('theme', action.payload);
        },
        toggleAutoTheme: (state) => {
            console.log('[ConfigSlice] Toggling auto theme:', {
                previous: state.theme.autoSwitch,
                new: !state.theme.autoSwitch
            });
            state.theme.autoSwitch = !state.theme.autoSwitch;
        },
        updateWebSocketConfig: (state, action: PayloadAction<Partial<AppConfig['websocket']>>) => {
            console.log('[ConfigSlice] Updating WebSocket config:', {
                previous: state.websocket,
                updates: action.payload,
                merged: {...state.websocket, ...action.payload}
            });
            state.websocket = {...state.websocket, ...action.payload};
            // Persist to localStorage
            try {
                localStorage.setItem('websocketConfig', JSON.stringify(state.websocket));
                console.log('[ConfigSlice] WebSocket config persisted to localStorage');
            } catch (error) {
                console.error('[ConfigSlice] Failed to persist WebSocket config:', {
                    source: 'localStorage',
                    error
                });
            }
        },
        updateConfig: (state: AppConfig, action: PayloadAction<Partial<AppConfig>>) => {
            console.log('[ConfigSlice] Updating config:', {
                previous: state,
                updates: action.payload,
                merged: {...state, ...action.payload}
            });
            return {...state, ...action.payload};
        },
        toggleSingleInput: (state: AppConfig) => {
            console.log('[ConfigSlice] Toggling single input:', {
                previous: state.singleInput,
                new: !state.singleInput
            });
            state.singleInput = !state.singleInput;
        },
        toggleStickyInput: (state: AppConfig) => {
            console.log('[ConfigSlice] Toggling sticky input:', {
                previous: state.stickyInput,
                new: !state.stickyInput
            });
            state.stickyInput = !state.stickyInput;
        },
        toggleLoadImages: (state: AppConfig) => {
            console.log('[ConfigSlice] Toggling load images:', {
                previous: state.loadImages,
                new: !state.loadImages
            });
            state.loadImages = !state.loadImages;
        },
        toggleMenubar: (state: AppConfig) => {
            console.log('[ConfigSlice] Toggling menubar:', {
                previous: state.showMenubar,
                new: !state.showMenubar
            });
            state.showMenubar = !state.showMenubar;
        },
        setApplicationName: (state: AppConfig, action: PayloadAction<string>) => {
            console.log('[ConfigSlice] Setting application name:', {
                previous: state.applicationName,
                new: action.payload
            });
            state.applicationName = action.payload;
        },
    },
});

export const {
    updateConfig,
    toggleSingleInput,
    toggleStickyInput,
    toggleLoadImages,
    toggleMenubar,
    setApplicationName,
    updateWebSocketConfig,
} = configSlice.actions;

export default configSlice.reducer;