import React, {useCallback} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import {ThemeName} from '../types';
import {setTheme} from '../store/slices/uiSlice';
import {RootState} from '../store';

export const useTheme = (initialTheme?: ThemeName): [ThemeName, (theme: ThemeName) => void] => {
    console.log('useTheme hook initialized with:', {initialTheme});

    const dispatch = useDispatch();
    const currentTheme = useSelector((state: RootState) => state.ui.theme);
    console.log('Current theme from state:', currentTheme);

    const updateTheme = useCallback(
        (newTheme: ThemeName) => {
            console.log('Updating theme to:', newTheme);
            dispatch(setTheme(newTheme));
            localStorage.setItem('theme', newTheme);
            console.log('Theme updated in localStorage');
        },
        [dispatch]
    );

    // Use initialTheme if provided and no theme is set in state
    React.useEffect(() => {
        console.log('Theme effect running with:', {initialTheme, currentTheme});
        if (initialTheme && !currentTheme) {
            console.log('Setting initial theme:', initialTheme);
            updateTheme(initialTheme);
        }
    }, [initialTheme, currentTheme, updateTheme]);
    console.log('useTheme hook returning:', {currentTheme});

    return [currentTheme, updateTheme];
};