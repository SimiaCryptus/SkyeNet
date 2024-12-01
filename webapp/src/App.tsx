import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import ErrorBoundary from './components/ErrorBoundary/ErrorBoundary';
import ErrorFallback from './components/ErrorBoundary/ErrorFallback';
import './App.css';
import websocket from './services/websocket';
import { GlobalStyles } from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import ThemeProvider from './themes/ThemeProvider';
import {Menu} from "./components/Menu/Menu";
import {Modal} from "./components/Modal/Modal";
import {setupUIHandlers} from './utils/uiHandlers';
// Import Prism core
import Prism from 'prismjs';
// Import base CSS
// import 'prismjs/themes/prism.css';
// Import commonly used languages

import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-markup';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-jsx';
import 'prismjs/components/prism-tsx';
import 'prismjs/components/prism-diff';
import 'prismjs/components/prism-markdown';
import 'prismjs/components/prism-kotlin';
import 'prismjs/components/prism-java';
import 'prismjs/components/prism-mermaid';
import 'prismjs/components/prism-scala';
import 'prismjs/components/prism-python';

// Import essential plugins
import 'prismjs/plugins/toolbar/prism-toolbar';
import 'prismjs/plugins/toolbar/prism-toolbar.css';
import 'prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard';
import 'prismjs/plugins/line-numbers/prism-line-numbers';
import 'prismjs/plugins/line-numbers/prism-line-numbers.css';
import 'prismjs/plugins/line-highlight/prism-line-highlight';
import 'prismjs/plugins/line-highlight/prism-line-highlight.css';
import 'prismjs/plugins/diff-highlight/prism-diff-highlight';
import 'prismjs/plugins/diff-highlight/prism-diff-highlight.css';
import 'prismjs/plugins/show-language/prism-show-language';
import 'prismjs/plugins/normalize-whitespace/prism-normalize-whitespace';
// import 'prismjs/plugins/autoloader/prism-autoloader';
import QRCode from 'qrcode-generator';

const APP_VERSION = '1.0.0';
const LOG_PREFIX = '[App]';
Prism.manual = true;


const App: React.FC = () => {
    console.group(`${LOG_PREFIX} Initializing v${APP_VERSION}`);
    console.log('Starting component render');

    const sessionId = websocket.getSessionId();
    const isConnected = websocket.isConnected();
    console.log('WebSocket state:', {
        sessionId,
        isConnected
    });

    React.useEffect(() => {
        console.log(`${LOG_PREFIX} Setting up handlers`);
        setupUIHandlers();
    }, []);

    React.useEffect(() => {
        console.log(`${LOG_PREFIX} Component mounted, initializing libraries`);
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
                                console.debug(`${LOG_PREFIX} Rendering ThemeProvider with theme`);
                                return (
                                    <>
                                        <div className={`App`}>
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
console.groupEnd();
console.log(`${LOG_PREFIX} v${APP_VERSION} loaded successfully`);


export default App;