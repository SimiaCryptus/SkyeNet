import {Provider} from 'react-redux';
import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import {store} from './store';
import './index.css';
import mermaid from 'mermaid';
import QRCode from 'qrcode-generator';

console.log('Application initializing...');


const rootElement = document.getElementById('root');
if (!rootElement) {
    console.error('Failed to find root element in DOM');
    throw new Error('Failed to find the root element');
}

const root = createRoot(rootElement);
console.log('React root created successfully');
mermaid.initialize({startOnLoad: true});
window.mermaid = mermaid; // Make mermaid globally accessible
window.QRCode = QRCode; // Make QRCode globally accessible
try {

    root.render(
        <Provider store={store}>
            <App/>
        </Provider>
    );
    console.log('Application rendered successfully');
} catch (error) {
    console.error('Failed to render application:', error);
    throw error;
}