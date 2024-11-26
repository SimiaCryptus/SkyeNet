// Define theme names
 export type ThemeName = 'main' | 'night' | 'forest' | 'pony' | 'alien';
// Define log levels
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

// Define log entry interface
export interface LogEntry {
    timestamp: number;
    level: LogLevel;
    message: string;
    data?: unknown;
    source?: string;
}


interface UIState {
  activeTab: string;
  modalOpen: boolean;
  modalType: string | null;
  verboseMode: boolean;
  theme: ThemeName;
    logHistory: LogEntry[];
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
 }
 // AppConfig type
 export interface AppConfig {
   singleInput: boolean;
   stickyInput: boolean;
   loadImages: boolean;
   showMenubar: boolean;
   applicationName?: string;
     logging: {
         enabled: boolean;
         level: LogLevel;
         maxEntries?: number;
         persistLogs?: boolean;
     };
 }
 // UserInfo type
 export interface UserInfo {
   name: string;
   isAuthenticated: boolean;
   preferences?: Record<string, unknown>;
 }
 export interface WebSocketState {
   connected: boolean;
   connecting: boolean;
   error: string | null;
     lastLog?: LogEntry;
 }

// Logger service interface
export interface LoggerService {
    debug(message: string, data?: unknown): void;

    info(message: string, data?: unknown): void;

    warn(message: string, data?: unknown): void;

    error(message: string, data?: unknown): void;

    getHistory(): LogEntry[];

    clearHistory(): void;
}

// Export types
export type {
  UIState
};