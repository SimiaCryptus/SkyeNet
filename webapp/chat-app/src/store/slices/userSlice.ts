import {createSlice, PayloadAction} from '@reduxjs/toolkit';
import {UserInfo} from '../../types';

const initialState: UserInfo = {
    name: '',
    isAuthenticated: false,
    preferences: {},
};

const logStateChange = (actionName: string, prevState: UserInfo, newState: UserInfo) => {
    console.group(`%c🔄 User State Change: ${actionName}`, 'color: #2196F3; font-weight: bold;');
    console.log('%c📤 Previous State:', 'color: #f44336', prevState);
    console.log('%c📥 New State:', 'color: #4CAF50', newState);
    // Log specific changes
    const changes = Object.keys(newState).reduce((acc: Record<string, { old: any, new: any }>, key) => {
        if (JSON.stringify(prevState[key as keyof UserInfo]) !== JSON.stringify(newState[key as keyof UserInfo])) {
            acc[key] = {
                old: prevState[key as keyof UserInfo],
                new: newState[key as keyof UserInfo]
            };
        }
        return acc;
    }, {});
    if (Object.keys(changes).length > 0) {
        console.log('%c📝 Changed Properties:', 'color: #FF9800', changes);
    }
    // Add timestamp
    console.log('%c⏰ Timestamp:', 'color: #9C27B0', new Date().toISOString());
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
            state.preferences = {...state.preferences, ...action.payload};
            logStateChange('updatePreferences', prevState, state);
        },
    },
});

export const {setUser, login, logout, updatePreferences} = userSlice.actions;

export default userSlice.reducer;