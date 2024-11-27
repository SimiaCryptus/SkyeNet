import {Provider} from 'react-redux';
import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import {store} from './store';
import './index.css';
import mermaid from 'mermaid';
import QRCode from 'qrcode-generator';

console.log('[App] Starting application initialization...');
console.log('[App] Redux store:', store);


const rootElement = document.getElementById('root');
if (!rootElement) {
    console.error('[App] Critical Error: Failed to find root element in DOM');
    throw new Error('Failed to find the root element');
}
console.log('[App] Root element found, creating React root...');

const root = createRoot(rootElement);
console.log('[App] React root created successfully');
console.log('[App] Initializing mermaid...');
mermaid.initialize({startOnLoad: true});
window.mermaid = mermaid; // Make mermaid globally accessible
window.QRCode = QRCode; // Make QRCode globally accessible
console.log('[App] Mermaid initialized, QRCode ready');
try {
    console.log('[App] Starting application render...');

    root.render(
        <Provider store={store}>
            <App/>
        </Provider>
    );
    console.log('[App] Application rendered successfully ✅');
} catch (error) {
    // Type guard to check if error is an Error object
    const err = error as Error;
    console.error('[App] Critical Error: Failed to render application:', {
        error: err,
        errorMessage: err.message,
        errorStack: err.stack
    });
    throw error;
}
console.log('[App] Initialization complete 🚀');