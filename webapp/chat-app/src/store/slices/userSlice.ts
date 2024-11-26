import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {UserInfo} from '../../types';

const initialState: UserInfo = {
  name: '',
  isAuthenticated: false,
  preferences: {},
};
// Helper function for logging state changes
const logStateChange = (actionName: string, prevState: UserInfo, newState: UserInfo) => {
  console.group(`User State Change: ${actionName}`);
  console.log('Previous State:', prevState);
  console.log('New State:', newState);
  console.groupEnd();
};


const userSlice = createSlice({
  name: 'user',
  initialState,
  reducers: {
    setUser: (state: UserInfo, action: PayloadAction<UserInfo>) => {
      const newState = {...state, ...action.payload};
      logStateChange('setUser', state, newState);
      return newState;
    },
    login: (state: UserInfo, action: PayloadAction<{ name: string }>) => {
      const prevState = {...state};
      state.name = action.payload.name;
      state.isAuthenticated = true;
      logStateChange('login', prevState, state);
    },
    logout: (state: UserInfo) => {
      const prevState = {...state};
      state.name = '';
      state.isAuthenticated = false;
      state.preferences = {};
      logStateChange('logout', prevState, state);
    },
    updatePreferences: (state: UserInfo, action: PayloadAction<Record<string, unknown>>) => {
      const prevState = {...state};
      state.preferences = { ...state.preferences, ...action.payload };
      logStateChange('updatePreferences', prevState, state);
    },
  },
});

export const { setUser, login, logout, updatePreferences } = userSlice.actions;

export default userSlice.reducer;