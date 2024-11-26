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
        console: {
            background: string;
            text: string;
            error: string;
            warning: string;
            success: string;
            info: string;
            debug: string;
        };
        console: {
            background: string;
            text: string;
            error: string;
            warning: string;
            success: string;
            info: string;
            debug: string;
        };
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
        console: {
            minHeight: string;
            maxHeight: string;
            padding: string;
        };
        console: {
            minHeight: string;
            maxHeight: string;
            padding: string;
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
        console: {
            fontFamily: string;
            fontSize: string;
            lineHeight: string;
        };
    };
    name: string;
      logging?: {
          colors: {
              error: string;
              warning: string;
              info: string;
              debug: string;
              success: string;
          };
          fontSize: {
              normal: string;
              large: string;
          };
          padding: {
              message: string;
          };
      };
  }
}