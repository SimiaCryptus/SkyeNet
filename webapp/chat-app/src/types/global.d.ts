// Extend the Window interface to include the mermaid property
declare global {
    interface HTMLElement {
        _contentObserver?: MutationObserver;
    }
}

export type {AppConfig};