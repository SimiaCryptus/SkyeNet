// Import theme types
import { ThemeName } from '../themes/themes';

// Message type
export interface Message {
  id: string;
  content: string;
  type: 'user' | 'system' | 'response';
  version: string;
  parentId?: string;
  timestamp: number;
}
// AppConfig type 

export interface AppConfig {
  singleInput: boolean;
  stickyInput: boolean;
  loadImages: boolean;
  showMenubar: boolean;
  applicationName?: string;
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
}

export interface UIState {
  activeTab: string;
  modalOpen: boolean;
  modalType: string | null;
  verboseMode: boolean;
  theme: ThemeName;
}

// Re-export ThemeName as Theme
export type { ThemeName as Theme };