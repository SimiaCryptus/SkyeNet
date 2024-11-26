import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import websocket from './services/websocket';
import {GlobalStyles} from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import ThemeProvider from './themes/ThemeProvider';
import {Menu} from "./components/Menu/Menu";

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