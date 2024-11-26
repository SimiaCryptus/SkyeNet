import React, {useEffect} from 'react';
import styled, {DefaultTheme} from 'styled-components';
import {Message} from '../../types';
import {logger} from '../../utils/logger';

const ChatContainer = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
`;

const MessageList = styled.div`
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
`;

const InputArea = styled.div<{ theme: DefaultTheme }>`
  padding: 1rem;
  border-top: 1px solid ${(props: { theme: DefaultTheme }) => props.theme.colors.border};
`;

interface ChatProps {
  messages: Message[];
  onSendMessage: (content: string) => void;
}

const Chat: React.FC<ChatProps> = ({ messages, onSendMessage }) => {
  // Log component mount and updates
  useEffect(() => {
    logger.component('Chat', 'Component mounted');
    logger.component('Chat', 'Initial messages:', messages);
    return () => {
      logger.component('Chat', 'Component unmounting');
    };
  }, []);
  // Log when messages change
  useEffect(() => {
    console.log('[Chat] Messages updated:', messages);
  }, [messages]);
  // Wrap onSendMessage to add logging
  const handleSendMessage = (content: string) => {
    console.log('[Chat] Sending message:', content);
    onSendMessage(content);
  };
  // Log render
  console.log('[Chat] Rendering with', messages.length, 'messages');

  return (
    <ChatContainer>
      <MessageList>
        {/* Message components will go here */}
      </MessageList>
      <InputArea>
        {/* Input component will go here */}
      </InputArea>
    </ChatContainer>
  );
};
// Add display name for better debugging
Chat.displayName = 'Chat';


export default Chat;