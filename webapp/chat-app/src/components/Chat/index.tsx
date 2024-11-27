import React, {useEffect} from 'react';
import styled from 'styled-components';
import {Message} from '../../types';
import MessageList from '../MessageList';
import InputArea from '../InputArea';
import {useSelector} from 'react-redux';
import {RootState} from '../../store';

const LOG_PREFIX = '[Chat]';


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
    console.group(`${LOG_PREFIX} Rendering Chat component`);

    const [activeTab, setActiveTab] = React.useState(() => {
        const savedTab = localStorage.getItem('activeTab');
        console.log(`${LOG_PREFIX} Initial active tab:`, {
            savedTab,
            fallback: 'chat'
        });
        return savedTab || 'chat';
    });
    // Validate tab on mount
    React.useEffect(() => {
        const validTabs = ['chat', 'files', 'settings'];
        console.log(`${LOG_PREFIX} Validating active tab:`, {
            current: activeTab,
            valid: validTabs.includes(activeTab),
            allowedTabs: validTabs
        });
        if (!validTabs.includes(activeTab)) {
            console.warn(`${LOG_PREFIX} Invalid active tab "${activeTab}". Resetting to chat.`);
            setActiveTab('chat');
        }
    }, []);
    // Add debug logging for active tab changes
    React.useEffect(() => {
        console.log(`${LOG_PREFIX} Active tab changed:`, activeTab);
    }, [activeTab]);
    const config = useSelector((state: RootState) => state.config);
    // Add error boundary
    const [error, setError] = React.useState<Error | null>(null);
    // Add error handling for messages prop
    const safeMessages = messages || [];
    const handleTabChange = (tabId: string) => {
        console.group(`${LOG_PREFIX} Tab Change Handler`);
        console.log('Current tab:', activeTab);
        console.log('Requested tab:', tabId);

        if (tabId === activeTab) {
            console.log('Tab already active - no change needed');
            console.groupEnd();
            return;
        }
        try {
            console.log(`${LOG_PREFIX} Changing tab to:`, tabId);
            setActiveTab(tabId);
            // Force a re-render of the tab content
            setTimeout(() => {
                const content = document.querySelector(`[data-tab="${tabId}"]`);
                if (content) {
                    content.classList.add('active');
                }
            }, 0);
            localStorage.setItem('activeTab', tabId);
            console.log(`${LOG_PREFIX} Tab change complete:`, {
                newTab: tabId,
                savedToStorage: true
            });
        } catch (error) {
            console.error(`${LOG_PREFIX} Error changing tab:`, error);
        }
        console.groupEnd();
    };
    // Add diagnostic logging for TabContent rendering
    const renderTabContent = (id: string, content: React.ReactNode) => {
        const isActive = activeTab === id;
        console.log(`${LOG_PREFIX} Rendering tab content:`, {
            id,
            isActive,
            hasContent: !!content
        });
        return (
            <TabContent
                key={id}
                data-tab={id}
                style={{display: isActive ? 'flex' : 'none'}}
                data-testid="chat-tabs"
            >
                {content}
            </TabContent>
        );
    };


    if (error) {
        console.error(`${LOG_PREFIX} Error encountered:`, error);
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
        console.group(`${LOG_PREFIX} Component Lifecycle`);
        console.log('Component mounted');
        console.log('Initial messages:', messages);
        console.groupEnd();

        return () => {
            console.log(`${LOG_PREFIX} Component unmounting`);
        };
    }, [messages]);
    // Save active tab to localStorage when it changes
    useEffect(() => {
        console.log(`${LOG_PREFIX} Tab changed:`, {
            activeTab,
            timestamp: new Date().toISOString()
        });
    }, [activeTab]);


    useEffect(() => {
        console.group(`${LOG_PREFIX} Messages Updated`);
        console.log('Count:', messages.length);
        console.log('Latest message:', messages[messages.length - 1]);
        console.log('Timestamp:', new Date().toISOString());
        console.groupEnd();
    }, [messages]);

    console.debug(`${LOG_PREFIX} Rendering`, {
        messageCount: messages.length,
        activeTab,
        hasError: !!error,
        configEnabled: !!config
    });

    return (
        <ChatContainer>
            <MessageList messages={safeMessages}/>
            <InputArea onSendMessage={onSendMessage}/>
        </ChatContainer>
    );
    console.groupEnd();
};
// Add display name for better debugging
Chat.displayName = 'Chat';


export default Chat;