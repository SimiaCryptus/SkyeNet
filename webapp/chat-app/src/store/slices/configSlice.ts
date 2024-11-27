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
            console.log('Loading saved WebSocket config from localStorage:', JSON.parse(savedConfig));
            return JSON.parse(savedConfig);
        }
    } catch (error) {
        console.error('Error loading WebSocket config from localStorage:', error);
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
        persistLogs: false
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
            console.log('Resetting config to initial state');
            return initialState;
        },
        setConnectionConfig: (state, action: PayloadAction<{
            retryAttempts: number;
            timeout: number;
        }>) => {
            state.websocket.retryAttempts = action.payload.retryAttempts;
            state.websocket.timeout = action.payload.timeout;
        },
        setTheme: (state, action: PayloadAction<ThemeName>) => {
            state.theme.current = action.payload;
            localStorage.setItem('theme', action.payload);
        },
        toggleAutoTheme: (state) => {
            state.theme.autoSwitch = !state.theme.autoSwitch;
        },
        updateWebSocketConfig: (state, action: PayloadAction<Partial<AppConfig['websocket']>>) => {
            console.log('Updating WebSocket config:', {
                current: state.websocket,
                updates: action.payload,
                new: {...state.websocket, ...action.payload}
            });
            state.websocket = {...state.websocket, ...action.payload};
            // Persist to localStorage
            try {
                localStorage.setItem('websocketConfig', JSON.stringify(state.websocket));
                console.log('WebSocket config saved to localStorage');
            } catch (error) {
                console.error('Error saving WebSocket config to localStorage:', error);
            }
        },
        updateConfig: (state: AppConfig, action: PayloadAction<Partial<AppConfig>>) => {
            console.log('Updating config:', {
                current: state,
                updates: action.payload,
                new: {...state, ...action.payload}
            });
            return {...state, ...action.payload};
        },
        toggleSingleInput: (state: AppConfig) => {
            console.log('Toggling single input:', {
                current: state.singleInput,
                new: !state.singleInput
            });
            state.singleInput = !state.singleInput;
        },
        toggleStickyInput: (state: AppConfig) => {
            console.log('Toggling sticky input:', {
                current: state.stickyInput,
                new: !state.stickyInput
            });
            state.stickyInput = !state.stickyInput;
        },
        toggleLoadImages: (state: AppConfig) => {
            console.log('Toggling load images:', {
                current: state.loadImages,
                new: !state.loadImages
            });
            state.loadImages = !state.loadImages;
        },
        toggleMenubar: (state: AppConfig) => {
            console.log('Toggling menubar:', {
                current: state.showMenubar,
                new: !state.showMenubar
            });
            state.showMenubar = !state.showMenubar;
        },
        setApplicationName: (state: AppConfig, action: PayloadAction<string>) => {
            console.log('Setting application name:', {
                current: state.applicationName,
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