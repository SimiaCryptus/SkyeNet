import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { UserInfo } from '../../types';

const initialState: UserInfo = {
  name: '',
  isAuthenticated: false,
  preferences: {},
};

const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state: UserInfo, action: PayloadAction<UserInfo>) => {
      return { ...state, ...action.payload };
    },
    login: (state: UserInfo, action: PayloadAction<{ name: string }>) => {
      state.name = action.payload.name;
      state.isAuthenticated = true;
    },
    logout: (state: UserInfo) => {
      state.name = '';
      state.isAuthenticated = false;
      state.preferences = {};
    },
    updatePreferences: (state: UserInfo, action: PayloadAction<Record<string, unknown>>) => {
      state.preferences = { ...state.preferences, ...action.payload };
    },
  },
});

export const { setUser, login, logout, updatePreferences } = userSlice.actions;

export default userSlice.reducer;