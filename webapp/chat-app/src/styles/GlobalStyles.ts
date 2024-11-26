import {createGlobalStyle, DefaultTheme} from 'styled-components';
// Log when global styles are being applied
const logThemeChange = (theme: DefaultTheme) => {
    console.log('Applying global styles with theme:', {
        background: theme.colors.background,
        textColor: theme.colors.text.primary,
        fontFamily: theme.typography.fontFamily,
        fontSize: theme.typography.fontSize.md
    });
};

export const GlobalStyles = createGlobalStyle<{ theme: DefaultTheme }>`
  * {
    margin: 0;
    padding: 0;
    box-sizing: border-box;
  }

  body {
      font-family: ${({theme}: { theme: DefaultTheme }) => {
          logThemeChange(theme);
          return theme.typography.fontFamily;
      }};
      background-color: ${({theme}: { theme: DefaultTheme }) => {
          console.log('Setting background color:', theme.colors.background);
          return theme.colors.background;
      }};
      color: ${({theme}: { theme: DefaultTheme }) => {
          console.log('Setting text color:', theme.colors.text.primary);
          return theme.colors.text.primary;
      }};
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
              console.log('Theme transition completed');
              return '';
          }}
      }
  }
`;