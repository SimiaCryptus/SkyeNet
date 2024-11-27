// Extend the Window interface to include the mermaid property
interface Window {
    mermaid: import('mermaid').Mermaid;
    QRCode: typeof import('qrcode-generator');
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