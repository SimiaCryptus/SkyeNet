// Define theme names
export type ThemeName = 'main' | 'night' | 'forest' | 'pony' | 'alien' | 'sunset' | 'ocean' | 'cyberpunk';

// Define log levels
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

// Define console styles
export interface ConsoleStyle {
    color?: string;
    background?: string;
    bold?: boolean;
    italic?: boolean;
    underline?: boolean;
}

// UserInfo type
export interface UserInfo {
    name: string;
    isAuthenticated: boolean;
    preferences?: Record<string, unknown>;
}