import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { AppConfig } from '../../types';

const initialState: AppConfig = {
  singleInput: false,
  stickyInput: true,
  loadImages: true,
  showMenubar: true,
  applicationName: 'Chat App',
};

const configSlice = createSlice({
  name: 'config',
  initialState,
  reducers: {
    updateConfig: (state: AppConfig, action: PayloadAction<Partial<AppConfig>>) => {
      return { ...state, ...action.payload };
    },
    toggleSingleInput: (state: AppConfig) => {
      state.singleInput = !state.singleInput;
    },
    toggleStickyInput: (state: AppConfig) => {
      state.stickyInput = !state.stickyInput;
    },
    toggleLoadImages: (state: AppConfig) => {
      state.loadImages = !state.loadImages;
    },
    toggleMenubar: (state: AppConfig) => {
      state.showMenubar = !state.showMenubar;
    },
    setApplicationName: (state: AppConfig, action: PayloadAction<string>) => {
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
} = configSlice.actions;

export default configSlice.reducer;