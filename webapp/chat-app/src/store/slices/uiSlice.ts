import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {ThemeName} from '../../themes/themes';

interface UiState {
  theme: ThemeName;
  modalOpen: boolean;
  modalType: string | null;
  verboseMode: boolean;
}

const initialState: UiState = {
  theme: 'main',
  modalOpen: false,
  modalType: null,
  verboseMode: false
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<ThemeName>) => {
      console.log('[UI Slice] Setting theme:', action.payload);
      state.theme = action.payload;
    },
    showModal: (state, action: PayloadAction<string>) => {
      console.log('[UI Slice] Showing modal:', action.payload);
      state.modalOpen = true;
      state.modalType = action.payload;
    },
    hideModal: (state) => {
      console.log('[UI Slice] Hiding modal');
      state.modalOpen = false;
      state.modalType = null;
    },
    toggleVerbose: (state) => {
      const newVerboseState = !state.verboseMode;
      console.log('[UI Slice] Toggling verbose mode:', newVerboseState);
      state.verboseMode = !state.verboseMode;
    }
  },
});

export const {setTheme, showModal, hideModal, toggleVerbose} = uiSlice.actions;
// Add debug logging for initial state
console.log('[UI Slice] Initialized with state:', initialState);

export default uiSlice.reducer;