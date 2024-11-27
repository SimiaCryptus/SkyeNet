import {createGlobalStyle, DefaultTheme} from 'styled-components';
// Enhanced logging function with timestamp
const logStyleChange = (component: string, property: string, value: any) => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] GlobalStyles: ${component} - ${property}:`, value);
};

// Log when global styles are being applied
const logThemeChange = (theme: DefaultTheme) => {
    const timestamp = new Date().toISOString();
    console.group(`[${timestamp}] GlobalStyles: Theme Application`);
    console.log('Theme configuration:', {
        background: theme.colors.background,
        textColor: theme.colors.text.primary,
        fontFamily: theme.typography.fontFamily,
        fontSize: theme.typography.fontSize.md
    });
    console.groupEnd();
};

export const GlobalStyles = createGlobalStyle<{ theme: DefaultTheme }>`
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
    }

    body {
        font-family: ${({theme}: { theme: DefaultTheme }) => {
            logStyleChange('body', 'font-family', theme.typography.fontFamily);
            return theme.typography.fontFamily;
        }};
        background-color: ${({theme}: { theme: DefaultTheme }) => {
            logStyleChange('body', 'background-color', theme.colors.background);
            return theme.colors.background;
        }};
        color: ${({theme}: { theme: DefaultTheme }) => {
            logStyleChange('body', 'color', theme.colors.text.primary);
            return theme.colors.text.primary;
        }};
        line-height: 1.5;
        font-size: ${({theme}: { theme: DefaultTheme }) => {
            logStyleChange('body', 'font-size', theme.typography.fontSize.md);
            return theme.typography.fontSize.md;
        }};
    }

    .chat-input {
        background-color: ${({theme}: { theme: DefaultTheme }) => theme.colors.surface};
        color: ${({theme}: { theme: DefaultTheme }) => theme.colors.text.primary};
        border-radius: ${({theme}: { theme: DefaultTheme }) => theme.sizing.borderRadius.md};
        padding: 10px;
        margin-bottom: 10px;
        overflow: auto;
        resize: vertical;
        border: 1px solid ${({theme}: { theme: DefaultTheme }) => theme.colors.border};
        box-shadow: 0 2px 5px rgba(0, 0, 0, 0.1);
        font-size: 16px;
        transition: border-color 0.3s ease;
        min-height: 40px;
    }

    .chat-input:focus {
        outline: none;
        border-color: ${({theme}: { theme: DefaultTheme }) => theme.colors.primary};
        box-shadow: 0 0 5px rgba(0, 123, 255, 0.5);
    }

    button {
        font-family: inherit;
        cursor: pointer;
    }

    input, textarea {
        font-family: inherit;
    }

    /* Transitions for theme switching */
    body, button, input, textarea {
        transition: background-color 0.3s ease, color 0.3s ease;
    }

    /* Log when transitions complete */
    body {
        &:after {
            content: '';
            transition: background-color 0.3s ease;
            opacity: 0;
        }

        &.theme-transition-complete:after {
            opacity: 1;
            ${() => {
                logStyleChange('body', 'transition', 'completed');
                return '';
            }}
        }
    }

    .cmd-button {
        display: inline-block;
        padding: 8px 15px;
        font-size: 14px;
        cursor: pointer;
        text-align: center;
        text-decoration: none;
        outline: none;
        color: #fff;
        background-color: #4CAF50;
        border: none;
        border-radius: 5px;
        box-shadow: 0 9px #999;
    }

    .cmd-button:hover {
        background-color: #3e8e41;
    }

    .cmd-button:active {
        background-color: #3e8e41;
        box-shadow: 0 5px #666;
        transform: translateY(4px);
    }
`;