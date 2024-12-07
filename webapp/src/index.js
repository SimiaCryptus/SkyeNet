import React from 'react';
import ReactDOM from 'react-dom/client';
import './index.css';
import App from './App';
import reportWebVitals from './reportWebVitals';

// Application initialization timestamp
const startTime = performance.now();
// Configure console styling
const logStyles = {
    startup: 'color: #4CAF50; font-weight: bold',
    error: 'color: #f44336; font-weight: bold',
    warning: 'color: #ff9800; font-weight: bold',
    info: 'color: #2196f3; font-weight: bold'
};



// Check if we're loading from an archive based on current document length
const isArchive = document.documentElement.outerHTML.length > 60000;

if (!isArchive) {
    console.log('%c[Chat App] Starting application...', logStyles.startup);
} else {
    console.log('%c[Chat App] Starting application in archive mode...', logStyles.startup);
}


if (typeof document !== 'undefined') {
    if (!isArchive) {
        console.log('%c[Chat App] Initializing React root element...', logStyles.info);
        const root = ReactDOM.createRoot(document.getElementById('root'));
        try {
            root.render(
                <React.StrictMode>
                    <App isArchive={isArchive}/>
                </React.StrictMode>
            );
            const renderTime = (performance.now() - startTime).toFixed(2);
            console.log(
                '%c[Chat App] Application rendered successfully in %cms',
                logStyles.startup,
                renderTime
            );
        } catch (error) {
            console.log(
                '%c[Chat App] Failed to render application:',
                logStyles.error,
                '\nError:', error,
                '\nStack:', error.stack
            );
        }
    }
} else {
    console.log(
        '%c[Chat App] Document is undefined - application may be running in a non-browser environment',
        logStyles.warning
    );
}
