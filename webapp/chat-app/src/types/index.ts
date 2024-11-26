// Import theme types
import {ThemeName} from '../themes/themes';
// Console logging levels
export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

// Console log entry interface
export interface LogEntry {
    timestamp: number;
    level: LogLevel;
    message: string;
    data?: unknown;
    source?: string;
}

// Console state interface
export interface ConsoleState {
    entries: LogEntry[];
    visible: boolean;
    filter: LogLevel[];
    maxEntries: number;
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
        maxEntries: number;
    };
}
// UserInfo type

export interface UserInfo {
  name: string;
  isAuthenticated: boolean;
  preferences?: Record<string, unknown>;
    debugMode?: boolean;
}

export interface WebSocketState {
  connected: boolean;
  connecting: boolean;
  error: string | null;
    lastMessageTimestamp?: number;
}

export interface UIState {
  activeTab: string;
  modalOpen: boolean;
  modalType: string | null;
  verboseMode: boolean;
  theme: ThemeName;
    consoleState: ConsoleState;
}

// Re-export ThemeName as Theme
export type { ThemeName as Theme };