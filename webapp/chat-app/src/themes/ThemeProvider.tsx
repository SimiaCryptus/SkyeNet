import React, {useEffect, useRef} from 'react';
import {ThemeProvider as StyledThemeProvider} from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {logThemeChange, themes} from './themes';

interface ThemeProviderProps {
    children: React.ReactNode;
}

const LOG_PREFIX = '[ThemeProvider]';

export const ThemeProvider: React.FC<ThemeProviderProps> = ({children}) => {
    const currentTheme = useSelector((state: RootState) => state.ui.theme);
    const isInitialMount = useRef(true);
    const previousTheme = useRef(currentTheme);

    useEffect(() => {
        if (isInitialMount.current) {
            console.info(`${LOG_PREFIX} Initial theme:`, currentTheme);
            isInitialMount.current = false;
        } else {
            logThemeChange(previousTheme.current, currentTheme);
            previousTheme.current = currentTheme;
            console.info(`${LOG_PREFIX} Theme changed to:`, currentTheme);
        }

        document.body.className = `theme-${currentTheme}`;
        // Add transition class
        document.body.classList.add('theme-transition');
        const timer = setTimeout(() => {
            document.body.classList.remove('theme-transition');
        }, 300);
        return () => clearTimeout(timer);
    }, [currentTheme]);

    const theme = themes[currentTheme] || themes.main;
    if (!themes[currentTheme]) {
        console.warn(
            `${LOG_PREFIX} Theme "${currentTheme}" not found. Falling back to main theme.`,
            '\nAvailable themes:', Object.keys(themes)
        );
    }

    return <StyledThemeProvider theme={theme}>{children}</StyledThemeProvider>;
};

// Log available themes on module load
console.info(`${LOG_PREFIX} Initialized with themes:`, Object.keys(themes));

export default ThemeProvider;