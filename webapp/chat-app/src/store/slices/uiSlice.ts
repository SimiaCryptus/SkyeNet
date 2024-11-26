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
      state.theme = action.payload;
    },
    showModal: (state, action: PayloadAction<string>) => {
      state.modalOpen = true;
      state.modalType = action.payload;
    },
    hideModal: (state) => {
      state.modalOpen = false;
      state.modalType = null;
    },
    toggleVerbose: (state) => {
      state.verboseMode = !state.verboseMode;
    }
  },
});

export const {setTheme, showModal, hideModal, toggleVerbose} = uiSlice.actions;
export default uiSlice.reducer;