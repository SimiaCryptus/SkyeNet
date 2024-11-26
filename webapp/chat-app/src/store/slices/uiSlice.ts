import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import { ThemeName } from '../../themes/themes';

interface UiState {
  theme: ThemeName;
}

const initialState: UiState = {
  theme: 'main'
};

const uiSlice = createSlice({
  name: 'ui',
  initialState,
  reducers: {
    setTheme: (state, action: PayloadAction<ThemeName>) => {
      state.theme = action.payload;
    }
  }
});

export const { setTheme } = uiSlice.actions;
export default uiSlice.reducer;
