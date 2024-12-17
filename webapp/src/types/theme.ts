// Core theme types
export type ThemeName = 'main' | 'night' | 'forest' | 'pony' | 'alien' | 'sunset' | 'ocean' | 'cyberpunk';

export interface ThemeColors {
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
    disabled: string;
    primaryDark?: string;
    hover?: string;
}

export interface ThemeSizing {
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
}

export interface ThemeTypography {
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
}

export interface ThemeLogging {
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
}

export interface ThemeConfig {
    stickyInput: boolean;
    singleInput: boolean;
}

export interface BaseTheme {
    name: string;
    colors: ThemeColors;
    sizing: ThemeSizing;
    typography: ThemeTypography;
    logging: ThemeLogging;
    config: ThemeConfig;
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
    activeTab?: string;
    _init?: () => void;
}