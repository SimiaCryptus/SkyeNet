import React, {useEffect} from 'react';
import styled from 'styled-components';
import {Message} from '../../types';
import {logger} from '../../utils/logger';
import Tabs from '../Tabs';
import MessageList from '../MessageList';
import InputArea from '../InputArea';
import {useSelector} from 'react-redux';
import {RootState} from '../../store';

const ChatContainer = styled.div`
    display: flex;
    flex-direction: column;
    height: 100%;
`;
const MainInput = styled.form`
    position: ${({theme}) => theme.config?.stickyInput ? 'sticky' : 'relative'};
    bottom: 0;
    display: ${({theme}) => theme.config?.singleInput ? 'none' : 'block'};
    z-index: 1;
    padding: 1rem;
    background: ${({theme}) => theme.colors.surface};
`;

const TabContent = styled.div`
    flex: 1;
    display: flex;
    flex-direction: column;
    overflow: hidden;
`;

interface ChatProps {
    messages: Message[];
    onSendMessage: (content: string) => void;
}

const Chat: React.FC<ChatProps> = ({messages, onSendMessage}) => {
    const [activeTab, setActiveTab] = React.useState('chat');
    const config = useSelector((state: RootState) => state.config);
    // Add error boundary
    const [error, setError] = React.useState<Error | null>(null);
    if (error) {
        return (
            <div className="error-boundary">
                <h2>Something went wrong</h2>
                <p>{error.message}</p>
                <button onClick={() => setError(null)}>Try again</button>
            </div>
        );
    }


    const tabs = [
        {id: 'chat', label: 'Chat'},
        {id: 'files', label: 'Files'},
        {id: 'settings', label: 'Settings'}
    ];

    // Log component mount and updates
    useEffect(() => {
        logger.component('Chat', 'Component mounted');
        logger.component('Chat', 'Initial messages:', messages);
        return () => {
            logger.component('Chat', 'Component unmounting');
        };
    }, []);
    // Save active tab to localStorage when it changes
    useEffect(() => {
        logger.component('Chat', 'Saved active tab:', activeTab);
    }, [activeTab]);


    useEffect(() => {
        console.log('[Chat] Messages updated:', messages);
    }, [messages]);

    console.log('[Chat] Rendering with', messages.length, 'messages');

    return (
        <ChatContainer>
            <Tabs tabs={tabs} activeTab={activeTab} onTabChange={setActiveTab}>
                <TabContent>
                    {activeTab === 'chat' && (
                        <>
                            <MessageList messages={messages}/>
                            <InputArea onSendMessage={onSendMessage}/>
                        </>
                    )}
                    {activeTab === 'files' && (
                        <div className="tab-content" data-tab="files">
                            <h2>Files</h2>
                            <p>Browse and manage your chat files here.</p>
                        </div>
                    )}
                    {activeTab === 'settings' && (
                        <div className="tab-content" data-tab="settings">
                            <h2>Settings</h2>
                            <p>Configure your chat preferences here.</p>
                        </div>
                    )}
                </TabContent>
            </Tabs>
        </ChatContainer>
    );
};
// Add display name for better debugging
Chat.displayName = 'Chat';


export default Chat;