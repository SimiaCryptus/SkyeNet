export type LogLevel = 'debug' | 'info' | 'warn' | 'error';


// Explicitly export MessageType
export type MessageType =
    | 'user'
    | 'assistant'
    | 'system'
    | 'error'
    | 'loading'
    | 'reference'
    | 'response';

export interface Message {
    id: string;
    content: string;
    type: MessageType;
    version: number;
    timestamp: number;
    parentId?: string;
    logLevel?: LogLevel;
    isHtml: boolean;
    rawHtml: string | null;
    sanitized: boolean;
}

export interface MessageUpdate {
    id: string;
    updates: Partial<Message>;
}

export interface MessageState {
    messages: Message[];
    pendingMessages: Message[];
    messageQueue: Message[];
    isProcessing: boolean;
    messageVersions: Record<string, number>;
    pendingUpdates?: Message[];
}