import React from 'react';
import {ThemeProvider as StyledThemeProvider} from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {themes} from './themes';

interface ThemeProviderProps {
    children: React.ReactNode;
}

export const ThemeProvider: React.FC<ThemeProviderProps> = ({children}) => {
    const currentTheme = useSelector((state: RootState) => state.ui.theme);
    console.log('[ThemeProvider] Current theme:', currentTheme);

    const theme = themes[currentTheme] || themes.main;
    if (!themes[currentTheme]) {
        console.warn('[ThemeProvider] Theme not found:', currentTheme, 'falling back to main theme');
    }
    console.debug('[ThemeProvider] Applied theme configuration:', theme);

    return <StyledThemeProvider theme={theme}>{children}</StyledThemeProvider>;
};
// Log available themes on module load
console.info('[ThemeProvider] Available themes:', Object.keys(themes));


export default ThemeProvider;