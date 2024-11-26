import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import websocket from './services/websocket';
import {GlobalStyles} from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import ThemeProvider from './themes/ThemeProvider';
import {Menu} from "./components/Menu/Menu";

const App: React.FC = () => {
    const sessionId = websocket.getSessionId();

    return (
        <Provider store={store}>
            <ThemeProvider>
                <GlobalStyles/>
                <div className="App">
                    <Menu/>
                    <ChatInterface 
                        sessionId={sessionId} 
                        websocket={websocket}
                        isConnected={websocket.isConnected()}
                    />
                </div>
            </ThemeProvider>
        </Provider>
    );
};

export default App;
