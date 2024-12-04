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
// Fallback theme in case of storage errors
const FALLBACK_THEME: ThemeName = 'main';

// Define Prism themes mapping to our theme names
const prismThemes: Record<ThemeName, string> = {
    main: 'prism',
    night: 'prism-dark',
    forest: 'prism-okaidia',
    pony: 'prism-twilight',
    alien: 'prism-tomorrow',
    sunset: 'prism-twilight', // Added missing sunset theme mapping
    ocean: 'prism-okaidia',  // Added comma
    cyberpunk: 'prism-tomorrow'  // Added cyberpunk theme mapping
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
        // Validate theme before applying
        if (!themes[currentTheme]) {
            console.warn(`${LOG_PREFIX} Invalid theme "${currentTheme}", falling back to ${FALLBACK_THEME}`);
            return;
        }

        // Create a style element for dynamic theme transitions
        const styleEl = document.createElement('style');
        document.head.appendChild(styleEl);
        // Add theme CSS variables to root
        styleEl.textContent = `
        :root {
            --theme-text-secondary: ${themes[currentTheme].colors.text.secondary};
            --theme-font-family: ${themes[currentTheme].typography.fontFamily};
            --theme-font-size-md: ${themes[currentTheme].typography.fontSize.md};
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

        // Add theme transition class to message content
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
        // Add transition class
        document.body.classList.add('theme-transition');
        // Force re-render of message content
        const bodyElements = document.querySelectorAll('.message-body');
        bodyElements.forEach(content => {
            content.classList.add('theme-transition');
        });


        // Load and apply Prism theme
        loadPrismTheme(currentTheme).then(() => {
            // Re-highlight all code blocks with new theme
            requestAnimationFrame(() => {
                Prism.highlightAll();
                // Apply theme variables to code blocks
                document.querySelectorAll('pre code').forEach(block => {
                    (block as HTMLElement).style.setProperty('--theme-background', themes[currentTheme].colors.background);
                    (block as HTMLElement).style.setProperty('--theme-text', themes[currentTheme].colors.text.primary);
                });
                // Update code block styles
                const codeBlocks = document.querySelectorAll('pre code');
                codeBlocks.forEach(block => {
                    (block as HTMLElement).classList.add('theme-transition');
                });
            });
        });
        const timer = setTimeout(() => {
            document.body.classList.remove('theme-transition');
            // Remove transition classes
            document.querySelectorAll('.theme-transition').forEach(el => {
                el.classList.remove('theme-transition');
                // Remove old theme classes but keep current
                Array.from(el.classList)
                    .filter(cls => cls.startsWith('theme-') && cls !== `theme-${currentTheme}`)
                    .forEach(cls => el.classList.remove(cls));
            });
            // Remove old theme classes from code blocks
            document.querySelectorAll('pre code').forEach(block => {
                Array.from(block.classList)
                    .filter(cls => cls.startsWith('theme-') && cls !== `theme-${currentTheme}`)
                    .forEach(cls => block.classList.remove(cls));
            });
        }, 300);
        return () => {
            clearTimeout(timer);
            styleEl.remove();
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