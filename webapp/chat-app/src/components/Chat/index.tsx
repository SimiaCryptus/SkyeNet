import React from 'react';
import styled from 'styled-components';
import { DefaultTheme } from 'styled-components';
import { Message } from '../../types';

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

export default Chat;