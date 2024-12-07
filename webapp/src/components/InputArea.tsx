import React, {memo, useCallback, useState} from 'react';
import styled, { css } from 'styled-components';
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

interface InputContainerProps {
    $hide?: boolean;
}
const InputContainer = styled.div<InputContainerProps>`
    padding: 1.5rem;
    background-color: ${(props) => props.theme.colors.surface};
    border-top: 1px solid ${(props) => props.theme.colors.border};
    display: ${({theme, $hide}) => $hide ? 'none' : 'block'};
    max-height: 10vh;
    position: sticky;
    bottom: 0;
    z-index: 10;
    backdrop-filter: blur(16px) saturate(180%);
    box-shadow: 0 -4px 16px rgba(0, 0, 0, 0.15);
    background: ${({theme}) => `linear-gradient(to top, 
        ${theme.colors.surface}dd,
        ${theme.colors.background}aa
    )`};
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
    transition: all 0.3s ease;
    background: ${({theme}) => theme.colors.background};

    &:focus {
        outline: none;
        border-color: ${(props) => props.theme.colors.primary};
        box-shadow: 0 0 0 2px ${({theme}) => `${theme.colors.primary}40`};
        transform: translateY(-1px);
    }
    &:disabled {
        background-color: ${(props) => props.theme.colors.disabled};
        cursor: not-allowed;
    }
`;
const SendButton = styled.button`
    padding: 0.75rem 1.5rem;
    background: ${({theme}) => `linear-gradient(135deg, 
        ${theme.colors.primary}, 
        ${theme.colors.primaryDark}
    )`};
    color: white;
    border: none;
    border-radius: ${(props) => props.theme.sizing.borderRadius.md};
    cursor: pointer;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    font-weight: ${({theme}) => theme.typography.fontWeight.medium};
    text-transform: uppercase;
    letter-spacing: 0.5px;
    position: relative;
    overflow: hidden;
    min-width: 120px;

    &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
    }
    &:hover:not(:disabled) {
        background: ${({theme}) => `linear-gradient(135deg,
            ${theme.colors.primaryDark},
            ${theme.colors.primary}
        )`};
        transform: translateY(-2px);
        box-shadow: 0 8px 16px ${({theme}) => theme.colors.primary + '40'};
    }

    &:active:not(:disabled) {
        transform: translateY(0);
    }

    &:after {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: linear-gradient(rgba(255, 255, 255, 0.2), transparent);
        pointer-events: none;
    }
`;

interface InputAreaProps {
    onSendMessage: (message: string) => void;
}

const InputArea = memo(function InputArea({onSendMessage}: InputAreaProps) {
    log('Initializing component');
    const [message, setMessage] = useState('');
    const config = useSelector((state: RootState) => state.config);
    const messages = useSelector((state: RootState) => state.messages.messages);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const textAreaRef = React.useRef<HTMLTextAreaElement>(null);
    const shouldHideInput = config.singleInput && messages.length > 0;

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
        // Focus the textarea when component mounts
        textAreaRef.current?.focus();
        return () => {
            log('Component unmounting');
        };
    }, [config]);


    return (
        <InputContainer $hide={shouldHideInput}>
            <StyledForm onSubmit={handleSubmit}>
                <TextArea
                    ref={textAreaRef}
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