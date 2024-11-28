import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {ThemeName} from '../../themes/themes';

interface UiState {
    theme: ThemeName;
    modalOpen: boolean;
    modalType: string | null;
    verboseMode: boolean;
    activeTab: string;
    lastUpdate?: number;
}

const initialState: UiState = {
    theme: 'main',
    modalOpen: false,
    modalType: null,
    verboseMode: localStorage.getItem('verboseMode') === 'true',
    activeTab: 'chat', // Set default tab
    lastUpdate: Date.now()
};

const logStateChange = (action: string, payload: any = null, prevState: any = null, newState: any = null) => {
    console.log(`[UI Slice] ${action}`, {
        ...(payload && {payload}),
        ...(prevState && {prevState}),
        ...(newState && {newState})
    });
};

const uiSlice = createSlice({
    name: 'ui',
    initialState,
    reducers: {
        setActiveTab: (state, action: PayloadAction<string>) => {
            logStateChange('Setting active tab', action.payload, {activeTab: state.activeTab});
            state.activeTab = action.payload;
        },
        setTheme: (state, action: PayloadAction<ThemeName>) => {
            logStateChange('Setting theme', action.payload, {theme: state.theme});
            state.theme = action.payload;
            localStorage.setItem('theme', action.payload);
        },
        setDarkMode: (state, action: PayloadAction<boolean>) => {
            const newTheme = action.payload ? 'night' : 'main';
            logStateChange('Setting dark mode', {
                darkMode: action.payload,
                newTheme
            }, {currentTheme: state.theme});
            state.theme = newTheme;
            localStorage.setItem('theme', newTheme);
        },
        showModal: (state, action: PayloadAction<string>) => {
            logStateChange('Showing modal', {
                modalType: action.payload
            }, {
                modalOpen: state.modalOpen,
                modalType: state.modalType
            });
            state.modalOpen = true;
            state.modalType = action.payload;
        },
        hideModal: (state) => {
            logStateChange('Hiding modal', null, {
                modalOpen: state.modalOpen,
                modalType: state.modalType
            });
            state.modalOpen = false;
            state.modalType = null;
        },
        toggleVerbose: (state) => {
            const newVerboseState = !state.verboseMode;
            logStateChange('Toggling verbose mode', {
                newState: newVerboseState
            }, {
                previousState: state.verboseMode
            });
            localStorage.setItem('verboseMode', newVerboseState.toString());
            state.verboseMode = !state.verboseMode;
        }
    },
});

export const {setTheme, showModal, hideModal, toggleVerbose, setActiveTab} = uiSlice.actions;
logStateChange('Initialized slice', null, null, initialState);

export default uiSlice.reducer;