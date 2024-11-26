import {LogEntry, LogLevel} from '../types';

class Logger {
    private static instance: Logger;
    private logHistory: LogEntry[] = [];
    private logLevel: LogLevel = 'info';

    private constructor() {
        console.log('Logger initialized');
    }

    public static getInstance(): Logger {
        if (!Logger.instance) {
            Logger.instance = new Logger();
        }
        return Logger.instance;
    }

    public setLogLevel(level: LogLevel): void {
        this.logLevel = level;
        console.log(`Log level set to: ${level}`);
    }

    public debug(message: string, data?: unknown): void {
        this.log('debug', message, data);
    }

    public info(message: string, data?: unknown): void {
        this.log('info', message, data);
    }

    public warn(message: string, data?: unknown): void {
        this.log('warn', message, data);
    }

    public error(message: string, data?: unknown): void {
        this.log('error', message, data);
    }

    public component(name: string, message: string, data?: unknown): void {
        this.log('debug', `[${name}] ${message}`, data);
    }

    public getHistory(): LogEntry[] {
        return [...this.logHistory];
    }

    public clearHistory(): void {
        this.logHistory = [];
    }

    private log(level: LogLevel, message: string, data?: unknown): void {
        // Skip debug logs unless debug level is enabled
        if (level === 'debug' && this.logLevel !== 'debug') {
            return;
        }

        const entry: LogEntry = {
            timestamp: Date.now(),
            level,
            message,
            data
        };

        this.logHistory.push(entry);
        console[level](message, data || '');
    }
}

export const logger = Logger.getInstance();