import React from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
import {logger} from '../utils/logger';

const MessageListContainer = styled.div`
  flex: 1;
  overflow-y: auto;
  padding: 1rem;
  display: flex;
  flex-direction: column;
  gap: 1rem;
`;

const MessageItem = styled.div<{ type: 'user' | 'system' | 'response' }>`
  padding: 0.5rem 1rem;
  border-radius: 8px;
  max-width: 80%;
  align-self: ${({ type }) => type === 'user' ? 'flex-end' : 'flex-start'};
  background-color: ${({ type }) => {
    switch (type) {
      case 'user':
        return '#007bff';
      case 'system':
        return '#6c757d';
      default:
        return '#f8f9fa';
    }
  }};
  color: ${({ type }) => type === 'user' || type === 'system' ? '#fff' : '#212529'};
`;

const MessageList: React.FC = () => {
  logger.component('MessageList', 'Rendering component');
  // Log when component is mounted/unmounted
  React.useEffect(() => {
    logger.component('MessageList', 'Component mounted');
    return () => {
      logger.component('MessageList', 'Component unmounted');
    };
  }, []);
  const messages = useSelector((state: RootState) => state.messages.messages);
  React.useEffect(() => {
    logger.debug('MessageList - Messages updated', messages);
  }, [messages]);

  return (
    <MessageListContainer>
      {messages.map((message) => {
        logger.debug('MessageList - Rendering message', message);
        return (
            <MessageItem key={`${message.id}-${message.timestamp}`} type={message.type}>
              <div className="message-body" dangerouslySetInnerHTML={{__html: message.content}}/>
            </MessageItem>
        );
      })}
    </MessageListContainer>
  );
};


export default MessageList;