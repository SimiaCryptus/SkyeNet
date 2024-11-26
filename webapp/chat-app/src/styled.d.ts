import 'styled-components';
// Extend the styled-components DefaultTheme interface
declare module 'styled-components' {
    export interface DefaultTheme {
        // Add console logging related properties
        logging: {
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
        // Add other theme properties as needed
        colors: {
            primary: string;
            secondary: string;
            background: string;
            text: {
                primary: string;
                secondary: string;
            };
            border: string;
            surface: string;
            error: string;
            success: string;
            warning: string;
            info: string;
        };
    }
}