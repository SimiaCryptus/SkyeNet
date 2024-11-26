import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {AppConfig} from '../../types';

const initialState: AppConfig = {
  singleInput: false,
  stickyInput: true,
  loadImages: true,
  showMenubar: true,
  applicationName: 'Chat App',
  logging: {
    enabled: true,
    level: 'info',
    maxEntries: 1000,
    persistLogs: false
  }
};

const configSlice = createSlice({
  name: 'config',
  initialState,
  reducers: {
    updateConfig: (state: AppConfig, action: PayloadAction<Partial<AppConfig>>) => {
      console.log('Updating config:', {
        current: state,
        updates: action.payload,
        new: {...state, ...action.payload}
      });
      return { ...state, ...action.payload };
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
} = configSlice.actions;

export default configSlice.reducer;