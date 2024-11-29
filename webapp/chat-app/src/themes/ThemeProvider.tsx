import React, {useEffect, useRef} from 'react';
import {ThemeProvider as StyledThemeProvider} from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {logThemeChange, themes, ThemeName} from './themes';
import Prism from 'prismjs';

interface ThemeProviderProps {
    children: React.ReactNode;
}

const LOG_PREFIX = '[ThemeProvider]';
// Define Prism themes mapping to our theme names
const prismThemes: Record<ThemeName, string> = {
    main: 'prism',
    night: 'prism-dark',
    forest: 'prism-okaidia',
    pony: 'prism-twilight',
    alien: 'prism-tomorrow'
};
// Function to load Prism theme
const loadPrismTheme = async (themeName: ThemeName) => {
    const prismTheme = prismThemes[themeName] || 'prism';
    try {
        await import(`prismjs/themes/${prismTheme}.css`);
        console.log(`${LOG_PREFIX} Loaded Prism theme: ${prismTheme}`);
    } catch (error) {
        console.warn(`${LOG_PREFIX} Failed to load Prism theme: ${prismTheme}`, error);
    }
};

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
        // Load and apply Prism theme
        loadPrismTheme(currentTheme).then(() => {
            // Re-highlight all code blocks with new theme
            requestAnimationFrame(() => {
                Prism.highlightAll();
            });
        });
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