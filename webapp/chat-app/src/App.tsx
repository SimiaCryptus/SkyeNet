import React from 'react';
import {Provider} from 'react-redux';
import {store} from './store';
import websocket from './services/websocket';
import {GlobalStyles} from './styles/GlobalStyles';
import ChatInterface from './components/ChatInterface';
import Header from "./components/Header";
import {setTheme} from "./store/slices/uiSlice";
import ThemeProvider from './themes/ThemeProvider';

const App: React.FC = () => {
    const sessionId = websocket.getSessionId();

    return (
        <Provider store={store}>
            <ThemeProvider>
                <GlobalStyles/>
                <div className="App">
                    <Header onThemeChange={setTheme}/>
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