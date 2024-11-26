import React, {useCallback} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {ThemeName} from '../types';
import {setTheme} from '../store/slices/uiSlice';
import {RootState} from '../store';

export const useTheme = (initialTheme?: ThemeName): [ThemeName, (theme: ThemeName) => void] => {
  const dispatch = useDispatch();
  const currentTheme = useSelector((state: RootState) => state.ui.theme);

    const updateTheme = useCallback(
        (newTheme: ThemeName) => {
            dispatch(setTheme(newTheme));
            localStorage.setItem('theme', newTheme);
        },
        [dispatch]
    );

    // Use initialTheme if provided and no theme is set in state
  React.useEffect(() => {
    if (initialTheme && !currentTheme) {
      updateTheme(initialTheme);
    }
  }, [initialTheme, currentTheme, updateTheme]);

  return [currentTheme, updateTheme];
};
