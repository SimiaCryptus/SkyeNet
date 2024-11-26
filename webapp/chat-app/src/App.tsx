import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import CodeHighlighter from './components/CodeHighlighter';
import MermaidDiagram from './components/MermaidDiagram';
import websocket from './services/websocket';
import {GlobalStyles} from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import ThemeProvider from './themes/ThemeProvider';
import {Menu} from "./components/Menu/Menu";
import {Modal} from "./components/Modal/Modal";
import Prism from 'prismjs';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-css';
import 'prismjs/components/prism-markup';
import 'prismjs/plugins/toolbar/prism-toolbar';
import 'prismjs/plugins/copy-to-clipboard/prism-copy-to-clipboard';
import 'prismjs/plugins/line-numbers/prism-line-numbers';
import 'prismjs/plugins/match-braces/prism-match-braces';
import 'prismjs/plugins/show-language/prism-show-language';
import mermaid from 'mermaid';
import QRCode from 'qrcode-generator';

const App: React.FC = () => {
    console.log('[App] Component rendering');

    const sessionId = websocket.getSessionId();
    const isConnected = websocket.isConnected();
    console.log('[App] WebSocket state:', {
        sessionId,
        isConnected,
    });
    React.useEffect(() => {
        console.log('[App] Component mounted');
        Prism.highlightAll();
        Prism.highlightAll();
        mermaid.run();
        // Example of generating a QR code
        const qr = QRCode(0, 'L');
        qr.addData('https://example.com');
        qr.make();
        console.log(qr.createImgTag());
        return () => {
            console.log('[App] Component unmounting');
        };
    }, []);

    return (
        <Provider store={store}>
            {(() => {
                console.log('[App] Rendering Provider with store');
                return (
                    <ThemeProvider>
                        {(() => {
                            console.log('[App] Rendering ThemeProvider');
                            return (
                                <>
                                    <GlobalStyles/>
                                    <div className="App">
                                        <Menu/>
                                        <ChatInterface
                                            sessionId={sessionId}
                                            websocket={websocket}
                                            isConnected={isConnected}
                                        />
                                        <CodeHighlighter code={`const hello = "world";`}/>
                                        <MermaidDiagram chart={`graph TD; A-->B; A-->C; B-->D; C-->D;`}/>
                                        <Modal/>
                                    </div>
                                </>
                            );
                        })()}
                    </ThemeProvider>
                );
            })()}
        </Provider>
    );
};
// Add version info to help with debugging
console.log('[App] Version 1.0.0 loaded');


export default App;