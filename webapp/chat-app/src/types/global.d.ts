import type {MermaidAPI} from 'mermaid';
import type QRCodeGenerator from 'qrcode-generator';
import type Prism from 'prismjs';

// Extend the Window interface to include the mermaid property
declare global {
    interface HTMLElement {
        _contentObserver?: MutationObserver;
    }

    interface Window {
        mermaid: MermaidAPI;
        QRCode: typeof QRCodeGenerator;
        Prism: typeof Prism;
        appConfig?: {
            singleInput: boolean;
            stickyInput: boolean;
            loadImages: boolean;
            showMenubar: boolean;
            websocket: {
                url: string;
                port: string;
                protocol: string;
            };
        };
        // Extended console interface with additional logging methods
        console: {
            debug(...args: any[]): void;
            info(...args: any[]): void;
            warn(...args: any[]): void;
            error(...args: any[]): void;
            trace(...args: any[]): void;
            // Add custom console methods
            group(label?: string): void;
            groupEnd(): void;
            table(tabularData: any, properties?: string[]): void;
            time(label: string): void;
            timeEnd(label: string): void;
        };
    }
}

interface AppConfig {
    singleInput: boolean;
    stickyInput: boolean;
    loadImages: boolean;
    showMenubar: boolean;
    applicationName: string;
    websocket: WebSocketConfig;
    appInfo: any | null;
    // ... other existing properties
}

export type {AppConfig};