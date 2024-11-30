import 'styled-components';
import {HTMLAttributes} from 'react';

declare module 'styled-components' {
    export interface StyledComponentProps<T> extends HTMLAttributes<T> {
        'data-tab'?: string;
        theme?: DefaultTheme;
    }

    export interface DefaultTheme {
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
        };
        shadows: {
            small: string;
            medium: string;
            large: string;
        };
        transitions: {
            default: string;
            fast: string;
            slow: string;
        };
        shadows: {
            small: string;
            medium: string;
            large: string;
        };
        transitions: {
            default: string;
            fast: string;
            slow: string;
        };
        typography: {
            fontFamily: string;
            monoFontFamily?: string;
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
            hover?: string;
            primaryDark?: string;
            disabled: string;  // Keep as required to match ExtendedTheme
        };
        name: string;
        activeTab?: string;
        config: {
            stickyInput: boolean;
            singleInput: boolean;
        };
        logging: {
            colors: {
                error: string;
                warning: string;
                info: string;
                debug: string;
                success: string;
                trace: string;
                verbose: string;
                system: string;
            };
            fontSize: {
                normal: string;
                large: string;
                small: string;
                system: string;
            };
            padding: {
                message: string;
                container: string;
                timestamp: string;
            };
            background: {
                error: string;
                warning: string;
                info: string;
                debug: string;
                success: string;
                system: string;
            };
            border: {
                radius: string;
                style: string;
                width: string;
            };
            timestamp: {
                format: string;
                color: string;
            };
        };
    }
}