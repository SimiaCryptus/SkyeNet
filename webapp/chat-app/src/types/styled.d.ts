import 'styled-components';

declare module 'styled-components' {
  export interface DefaultTheme {
    colors: {
      primary: string;
      secondary: string;
      background: string;
      surface: string;
      text: {
        primary: string;
        secondary: string;
      };
      border: string;
      error: string;
      success: string;
      warning: string;
      info: string;
    };
    sizing: {
      spacing: {
        xs: string;
        sm: string;
        md: string;
        lg: string;
        xl: string;
      };
      borderRadius: {
        sm: string;
        md: string;
        lg: string;
      };
    };
    typography: {
      fontFamily: string;
      fontSize: {
        xs: string;
        sm: string;
        md: string;
        lg: string;
        xl: string;
      };
      fontWeight: {
        regular: number;
        medium: number;
        bold: number;
      };
    };
    name: string;
  }
}