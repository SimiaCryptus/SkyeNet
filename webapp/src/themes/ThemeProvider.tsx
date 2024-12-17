import React, {useEffect, useRef} from 'react';
import {ThemeProvider as StyledThemeProvider} from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {logThemeChange, ThemeName, themes} from './themes';
import Prism from 'prismjs';
import {GlobalStyles} from "../styles/GlobalStyles";

interface ThemeProviderProps {
    children: React.ReactNode;
}

const LOG_PREFIX = '[ThemeProvider]';
const FALLBACK_THEME: ThemeName = 'main';
const prismThemes: Record<ThemeName, string> = {
    main: 'prism',
    night: 'prism-dark',
    forest: 'prism-okaidia',
    pony: 'prism-twilight',
    alien: 'prism-tomorrow',
    sunset: 'prism-twilight',
    ocean: 'prism-okaidia',
    cyberpunk: 'prism-tomorrow'
};

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
    const styleElRef = useRef<HTMLStyleElement | null>(null);

    useEffect(() => {
        if (!themes[currentTheme]) {
            console.warn(`${LOG_PREFIX} Invalid theme "${currentTheme}", falling back to ${FALLBACK_THEME}`);
            return;
        }

        if (!styleElRef.current) {
            styleElRef.current = document.createElement('style');
            document.head.appendChild(styleElRef.current);
        }
        const styleEl = styleElRef.current;
        requestAnimationFrame(() => {
            styleEl.textContent = `
        :root {
        }
        /* Theme-specific message content styles */
        .message-content {
            color: var(--theme-text);
            background: var(--theme-background);
        }
        .message-content pre,
        .message-content code {
            background: var(--theme-surface);
            border: 1px solid var(--theme-border);
            font-family: var(--theme-code-font);
        }
        `;
        });

        const contentElements = document.querySelectorAll('.message-content');
        contentElements.forEach(content => {
            content.classList.add('theme-transition');
        });
        if (isInitialMount.current) {
            console.info(`${LOG_PREFIX} Initial theme:`, currentTheme);
            isInitialMount.current = false;
        } else {
            logThemeChange(previousTheme.current, currentTheme);
            previousTheme.current = currentTheme;
            console.info(`${LOG_PREFIX} Theme changed to:`, currentTheme);
        }

        document.body.className = `theme-${currentTheme}`;
        // Add dynamic CSS rules for message content
        styleEl.textContent = `
        .message-content.theme-${currentTheme} {
            --theme-background: ${themes[currentTheme].colors.background};
            --theme-text: ${themes[currentTheme].colors.text.primary};
            --theme-surface: ${themes[currentTheme].colors.surface};
            --theme-primary: ${themes[currentTheme].colors.primary};
        }
        `;
        document.body.classList.add('theme-transition');
        const bodyElements = document.querySelectorAll('.message-body');
        bodyElements.forEach(content => {
            content.classList.add('theme-transition');
        });


        loadPrismTheme(currentTheme).then(() => {
            requestAnimationFrame(() => {
                const codeBlocks = document.querySelectorAll('pre code');
                const updates: (() => void)[] = [];
                codeBlocks.forEach(block => {
                    updates.push(() => {
                        (block as HTMLElement).style.setProperty('--theme-background', themes[currentTheme].colors.background);
                        (block as HTMLElement).style.setProperty('--theme-text', themes[currentTheme].colors.text.primary);
                        (block as HTMLElement).classList.add('theme-transition');
                    });
                });
                // Batch DOM updates
                requestAnimationFrame(() => {
                    updates.forEach(update => update());
                    Prism.highlightAll();
                });
            });
        });
        return () => {
            if (styleElRef.current) {
                styleElRef.current.remove();
                styleElRef.current = null;
            }
        };
    }, [currentTheme]);

    const theme = themes[currentTheme] || themes.main;
    if (!themes[currentTheme]) {
        console.warn(
            `${LOG_PREFIX} Theme "${currentTheme}" not found. Falling back to main theme.`,
            '\nAvailable themes:', Object.keys(themes)
        );
    }

    return (
        <StyledThemeProvider theme={theme}>
            <GlobalStyles theme={theme}/>{children}
        </StyledThemeProvider>);
};

// Log available themes on module load
console.info(`${LOG_PREFIX} Initialized with themes:`, Object.keys(themes));

export default ThemeProvider;