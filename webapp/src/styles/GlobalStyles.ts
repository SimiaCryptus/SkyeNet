import type {DefaultTheme} from 'styled-components';
import {createGlobalStyle} from 'styled-components';

// Enhanced logging function with timestamp
const logStyleChange = (component: string, property: string, value: any) => {
    const timestamp = new Date().toISOString();
    console.log(`[${timestamp}] GlobalStyles: ${component} - ${property}:`, value);
};

const GlobalStyles = createGlobalStyle<{ theme: DefaultTheme; }>`
    /* Improved scrollbar styling */
    ::-webkit-scrollbar {
        width: 10px;
    }

    ::-webkit-scrollbar-track {
        background: ${({theme}) => theme.colors.background};
        border-radius: 4px;
    }

    ::-webkit-scrollbar-thumb {
        background: ${({theme}) => theme.colors.primary + '40'};
        border-radius: 4px;
        border: 2px solid ${({theme}) => theme.colors.background};

        &:hover {
            background: ${({theme}) => theme.colors.primary + '60'};
        }
    }

    @import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');
    /* Theme CSS variables */
    :root {
        /* Theme variables are now set dynamically in ThemeProvider */
    }

    /* Override Prism.js theme colors to match current theme */
    .token.comment,
    .token.prolog,
    .token.doctype,
    .token.cdata {
        color: var(--theme-text-secondary);
    }

    .token.punctuation {
        color: var(--theme-text);
    }

    .token.property,
    .token.tag,
    .token.constant,
    .token.symbol {
        color: var(--theme-primary);
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
        font-family: 'Inter', -apple-system, BlinkMacSystemFont, sans-serif;
        background-color: var(--theme-background);
        color: var(--theme-text);
        line-height: 1.5;
        font-size: var(--theme-font-size-md);
        letter-spacing: -0.011em;
        text-rendering: optimizeLegibility;
        overflow-x: hidden;
        min-height: 100vh;
    }

    border-radius:

    12
    px

    !important
    ;
    padding:

    1.5
    em

    !important
    ;
    margin:

    1.5
    em

    0
    !important
    ;
    overflow: auto

    ;
    box-shadow:

    0
    8
    px

    24
    px

    rgba
    (
    0
    ,
    0
    ,
    0
    ,
    0.15
    )
    ;
    font-family:

    'Fira Code'
    ,
    Consolas, Monaco, monospace

    !important
    ;
    font-size:

    0.9
    em

    !important
    ;
    line-height:

    1.6
    !important
    ;
    border:

    1
    px solid ${({theme}) => theme.colors.border + '30'}

    ;

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
export {GlobalStyles};