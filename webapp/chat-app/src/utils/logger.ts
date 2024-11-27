import {LogEntry, LogLevel} from '../types';

class Logger {
    private readonly LOG_LEVELS = {
        debug: 0,
        info: 1,
        warn: 2,
        error: 3
    };
    private isDevelopment = process.env.NODE_ENV === 'development';
    private groupDepth = 0;
    private static instance: Logger;
    private logHistory: LogEntry[] = [];
    private logLevel: LogLevel = 'info';
    private readonly logLevelSeverity: Record<LogLevel, number> = {
        debug: 0,
        info: 1,
        warn: 2,
        error: 3
    };

    private constructor() {
        console.log('%cLogger initialized', 'color: #8a2be2; font-weight: bold;');
    }

    public static getInstance(): Logger {
        if (!Logger.instance) {
            Logger.instance = new Logger();
        }
        return Logger.instance;
    }

    public setLogLevel(level: LogLevel): void {
        this.logLevel = level;
        console.log(`%cLog level set to: ${level}`, 'color: #8a2be2; font-style: italic;');
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
        const formattedMessage = `%c[${name}]%c ${message}`;
        const formattedData = {
            styles: ['color: #4CAF50; font-weight: bold', 'color: inherit'],
            originalData: data
        };
        this.log('debug', formattedMessage, formattedData);
    }

    public getHistory(): LogEntry[] {
        return [...this.logHistory];
    }

    public clearHistory(): void {
        this.logHistory = [];
        console.clear();
        console.log('%cLog history cleared', 'color: #8a2be2; font-style: italic;');
    }

    public group(label: string): void {
        this.groupDepth++;
        console.group(label);
    }

    public groupEnd(): void {
        this.groupDepth = Math.max(0, this.groupDepth - 1);
        console.groupEnd();
    }

    private log(level: LogLevel, message: string, data?: unknown): void {
        // Skip non-critical logs in production
        if (!this.isDevelopment && level !== 'error') {
            return;
        }
        // Skip debug logs if not at debug level
        if (level === 'debug' && this.LOG_LEVELS[this.logLevel] > this.LOG_LEVELS.debug) {
            return;
        }

        const entry: LogEntry = {
            timestamp: Date.now(),
            level,
            message,
            data
        };

        this.logHistory.push(entry);
        const timestamp = new Date(entry.timestamp).toLocaleTimeString();
        const logStyles: Record<LogLevel, string> = {
            debug: 'color: #6c757d',
            info: 'color: #17a2b8',
            warn: 'color: #ffc107; font-weight: bold',
            error: 'color: #dc3545; font-weight: bold'
        };
        console[level](
            `%c${timestamp} ${'.'.repeat(this.groupDepth)}[${level}]:%c ${message}`,
            logStyles[level],
            'color: inherit',
            data || ''
        );
    }
}

export const logger = Logger.getInstance();