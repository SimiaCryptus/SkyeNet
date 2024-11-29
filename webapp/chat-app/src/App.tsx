import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import ErrorFallback from './components/ErrorBoundary/ErrorFallback';
import LoadingSpinner from './components/LoadingSpinner/LoadingSpinner';
import './App.css';
import websocket from './services/websocket';
import {GlobalStyles} from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import ThemeProvider from './themes/ThemeProvider';
import {Menu} from "./components/Menu/Menu";
import {Modal} from "./components/Modal/Modal";
import {setupUIHandlers} from './utils/uiHandlers';
// Import Prism core
import Prism from 'prismjs';
// Import base CSS
import 'prismjs/themes/prism.css';
// Import commonly used languages
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-markup';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-jsx';
import 'prismjs/components/prism-tsx';
// Import essential plugins
import 'prismjs/plugins/toolbar/prism-toolbar';
import 'prismjs/plugins/toolbar/prism-toolbar.css';
import 'prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard';
import 'prismjs/plugins/line-numbers/prism-line-numbers';
import 'prismjs/plugins/line-numbers/prism-line-numbers.css';
import mermaid from 'mermaid';
import QRCode from 'qrcode-generator';

const APP_VERSION = '1.0.0';
const LOG_PREFIX = '[App]';
// Configure Prism
Prism.manual = true;


const App: React.FC = () => {
    const [isLoading, setIsLoading] = React.useState(true);
    React.useEffect(() => {
        // Highlight code blocks after component mounts
        Promise.all([
            new Promise(resolve => {
                requestAnimationFrame(() => {
                    Prism.highlightAllUnder(document.body);
                    resolve(true);
                });
            }),
            mermaid.run()
        ]).finally(() => {
            setIsLoading(false);
        });
    }, []);
    console.group(`${LOG_PREFIX} Initializing v${APP_VERSION}`);
    console.log('Starting component render');

    const sessionId = websocket.getSessionId();
    const isConnected = websocket.isConnected();
    console.log('WebSocket state:', {
        sessionId,
        isConnected
    });

    // Initialize UI handlers and message handling
    React.useEffect(() => {
        console.log(`${LOG_PREFIX} Setting up handlers`);
        setupUIHandlers();
    }, []);

    React.useEffect(() => {
        console.log(`${LOG_PREFIX} Component mounted, initializing libraries`);
        // Initialize syntax highlighting
        Prism.highlightAll();
        console.log(`${LOG_PREFIX} Prism initialized`);
        // Initialize mermaid diagrams
        mermaid.run();
        console.log(`${LOG_PREFIX} Mermaid initialized`);
        // Initialize QR code generator
        const qr = QRCode(0, 'L');
        qr.addData('https://example.com');
        qr.make();
        console.log(`${LOG_PREFIX} QR Code generator initialized`);

        return () => {
            console.log(`${LOG_PREFIX} Component unmounting, cleaning up...`);
        };
    }, []);

    return (
        <ErrorBoundary FallbackComponent={ErrorFallback}>
            <Provider store={store}>
                {(() => {
                    console.debug(`${LOG_PREFIX} Rendering Provider with store`);
                    return (
                        <ThemeProvider>
                            {(() => {
                                console.debug(`${LOG_PREFIX} Rendering ThemeProvider`);
                                return (
                                    <>
                                        <GlobalStyles/>
                                        <div className={`App ${isLoading ? 'loading' : ''}`}>
                                            {isLoading && <LoadingSpinner/>}
                                            <Menu/>
                                            <ChatInterface
                                                sessionId={sessionId}
                                                websocket={websocket}
                                                isConnected={isConnected}
                                            />
                                            <Modal/>
                                        </div>
                                    </>
                                );
                            })()}
                        </ThemeProvider>
                    );
                })()}
            </Provider>
        </ErrorBoundary>
    );
};
// Close the logging group when component is loaded
console.groupEnd();
console.log(`${LOG_PREFIX} v${APP_VERSION} loaded successfully`);


export default App;