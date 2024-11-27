// Define theme names
export type ThemeName = 'main' | 'night' | 'forest' | 'pony' | 'alien';
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

// Define console config
export interface ConsoleConfig {
    enabled: boolean;
    showTimestamp: boolean;
    showLevel: boolean;
    showSource: boolean;
    styles: Record<LogLevel, ConsoleStyle>;
}


// Define log entry interface
export interface LogEntry {
    timestamp: number;
    level: LogLevel;
    message: string;
    data?: unknown;
    source?: string;
    stackTrace?: string;
    consoleOutput?: string;
}

// Message type
export interface Message {
    id: string;
    content: string;
    type: 'user' | 'system' | 'response';
    version: string;
    parentId?: string;
    timestamp: number;
    logLevel?: LogLevel;
    isHtml?: boolean;
    rawHtml?: string | null;
    sanitized?: boolean | null;
    consoleStyle?: ConsoleStyle;
}

// AppConfig type
export interface AppConfig {
    singleInput: boolean;
    stickyInput: boolean;
    loadImages: boolean;
    showMenubar: boolean;
    applicationName?: string;
    websocket: {
        url: string;
        port: string;
        protocol: string;
        retryAttempts?: number;
        timeout?: number;
    };
    logging: {
        enabled: boolean;
        level: LogLevel;
        maxEntries?: number;
        persistLogs?: boolean;
        console: ConsoleConfig;
        captureConsole?: boolean;
        logToFile?: boolean;
        logFilePath?: string;
        rotation?: {
            enabled: boolean;
            maxSize: number;
            maxFiles: number;
        };
    };
    theme: {
        current: ThemeName;
        autoSwitch: boolean;
    };
}

// UserInfo type
export interface UserInfo {
    name: string;
    isAuthenticated: boolean;
    preferences?: Record<string, unknown>;
}