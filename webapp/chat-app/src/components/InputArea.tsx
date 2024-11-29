import React, {useState, useCallback, memo} from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';

// Debug logging utility
const DEBUG = process.env.NODE_ENV === 'development';
const log = (message: string, data?: unknown) => {
    if (DEBUG) {
        if (data) {
            console.log(`[InputArea] ${message}`, data);
        } else {
            console.log(`[InputArea] ${message}`);
        }
    }
};

const InputContainer = styled.div`
    padding: 1rem;
    background-color: ${(props) => props.theme.colors.surface};
    border-top: 1px solid ${(props) => props.theme.colors.border};
    display: ${({theme}) => theme.config?.singleInput ? 'none' : 'block'};
    max-height: 10vh;
    position: sticky;
    bottom: 0;
    z-index: 10;
`;
const StyledForm = styled.form`
    display: flex;
    gap: 1rem;
    align-items: flex-start;
`;


const TextArea = styled.textarea`
    width: 100%;
    padding: 0.5rem;
    border-radius: ${(props) => props.theme.sizing.borderRadius.md};
    border: 1px solid ${(props) => props.theme.colors.border};
    font-family: inherit;
    resize: vertical;
    min-height: 40px;
    max-height: ${({theme}) => theme.sizing.console.maxHeight};
    &:focus {
        outline: 2px solid ${(props) => props.theme.colors.primary};
        border-color: ${(props) => props.theme.colors.primary};
    }
    &:disabled {
        background-color: ${(props) => props.theme.colors.disabled};
    }
`;
const SendButton = styled.button`
    padding: 0.5rem 1rem;
    background-color: ${(props) => props.theme.colors.primary};
    color: white;
    border: none;
    border-radius: ${(props) => props.theme.sizing.borderRadius.md};
    cursor: pointer;
    transition: opacity 0.2s;
    &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
    &:hover:not(:disabled) {
        opacity: 0.9;
    }
`;

interface InputAreaProps {
    onSendMessage: (message: string) => void;
}

const InputArea = memo(function InputArea({onSendMessage}: InputAreaProps) {
    log('Initializing component');
    const [message, setMessage] = useState('');
    const config = useSelector((state: RootState) => state.config);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = useCallback((e: React.FormEvent) => {
        e.preventDefault();
        if (isSubmitting) return;
        
        log('Submit attempt');
        if (message.trim()) {
            setIsSubmitting(true);
            log('Sending message', {
                messageLength: message.length,
                message: message.substring(0, 100) + (message.length > 100 ? '...' : '')
            });
            Promise.resolve(onSendMessage(message)).finally(() => {
                setMessage('');
                setIsSubmitting(false);
                log('Message sent and form reset');
            });
        } else {
            log('Empty message, not sending');
        }
    }, [message, onSendMessage]);

    const handleMessageChange = useCallback((e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const newMessage = e.target.value;
        log('Message changed', {
            length: newMessage.length,
            isEmpty: newMessage.trim().length === 0
        });
        setMessage(newMessage);
    }, []);

    const handleKeyPress = useCallback((e: React.KeyboardEvent<HTMLTextAreaElement>) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            handleSubmit(e);
        }
    }, [handleSubmit]);

    React.useEffect(() => {
        log('Component mounted', {configState: config});
        return () => {
            log('Component unmounting');
        };
    }, [config]);


    return (
        <InputContainer>
            <StyledForm onSubmit={handleSubmit}>
                <TextArea
                    value={message}
                    onChange={handleMessageChange}
                    onKeyPress={handleKeyPress}
                    placeholder="Type a message..."
                    rows={3}
                    aria-label="Message input"
                    disabled={isSubmitting}
                />
                <SendButton 
                    type="submit" 
                    disabled={isSubmitting || !message.trim()}
                    aria-label="Send message"
                >
                    Send
                </SendButton>
            </StyledForm>
        </InputContainer>
    );
});


export default InputArea;