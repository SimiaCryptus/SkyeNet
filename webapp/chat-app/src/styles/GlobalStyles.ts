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
    /* Theme CSS variables */
    :root {
        /* Add theme variables */
        --theme-background: ${({theme}) => theme.colors.background};
        --theme-text: ${({theme}) => theme.colors.text.primary};
        --theme-surface: ${({theme}) => theme.colors.surface};
        --theme-border: ${({theme}) => theme.colors.border};
        --theme-primary: ${({theme}) => theme.colors.primary};
    }
    /* Override Prism.js theme colors to match current theme */
    .token.comment,
    .token.prolog,
    .token.doctype,
    .token.cdata {
        color: ${({theme}) => theme.colors.text.secondary};
    }
    .token.punctuation {
        color: ${({theme}) => theme.colors.text.primary};
    }
    .token.property,
    .token.tag,
    .token.constant,
    .token.symbol {
        color: ${({theme}) => theme.colors.primary};
    }
    .token.boolean,
    .token.number {
        color: ${({theme}) => theme.colors.warning};
    }
    .token.selector,
    .token.string {
        color: ${({theme}) => theme.colors.success};
    }
    .token.operator,
    .token.keyword {
        color: ${({theme}) => theme.colors.info};
    }
    /* Reset styles */
    * {
        margin: 0;
        padding: 0;
        box-sizing: border-box;
        -webkit-font-smoothing: antialiased;
        -moz-osx-font-smoothing: grayscale;
    }

    /* Theme variables */

    :root {
        --transition-timing: cubic-bezier(0.4, 0, 0.2, 1);
        --transition-duration: 0.3s;
    }

    /* Improve focus styles globally */
    *:focus-visible {
        outline: 2px solid ${({theme}) => theme.colors.primary};
        outline-offset: 2px;
    }

    /* Improve button accessibility */
    button {
        font-family: inherit;
        font-size: inherit;
        line-height: inherit;
    }

    /* Message content theme transitions */
    .message-content {
        color: var(--theme-text);
        background: var(--theme-background);
        border-color: var(--theme-border);
    }
    .message-content pre,
    .message-content code {
        background: var(--theme-surface);
        color: var(--theme-text);
    }
    
    /* Universal code block styles using CSS variables */
    pre code {
        background: var(--theme-surface);
        color: var(--theme-text);
        border-color: var(--theme-border);
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
    background-color: ${({theme}) => theme.colors.primary};
        color: #fff;
        border: none;
        border-radius: 5px;
    box-shadow: ${({theme}) => theme.shadows.medium};
        transition: all ${({theme}) => theme.transitions?.default} var(--transition-timing);
    /* Inherit base styles from App.css */
    composes: cmd-button from global;
    }

    .cmd-button:hover {
    background-color: ${({theme}) => theme.colors.primaryDark};
        transform: translateY(-2px);
        box-shadow: ${({theme}) => theme.shadows?.large};
    }

    .cmd-button:active {
        transform: translateY(0);
        box-shadow: ${({theme}) => theme.shadows.medium};
    }
`;