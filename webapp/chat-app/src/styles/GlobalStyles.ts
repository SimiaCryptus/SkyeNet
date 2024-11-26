import { createGlobalStyle, DefaultTheme } from 'styled-components';

export const GlobalStyles = createGlobalStyle<{ theme: DefaultTheme }>`
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  body {
    font-family: ${({ theme }: { theme: DefaultTheme }) => theme.typography.fontFamily};
    background-color: ${({ theme }: { theme: DefaultTheme }) => theme.colors.background};
    color: ${({ theme }: { theme: DefaultTheme }) => theme.colors.text.primary};
    line-height: 1.5;
    font-size: ${({ theme }: { theme: DefaultTheme }) => theme.typography.fontSize.md};
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
`;