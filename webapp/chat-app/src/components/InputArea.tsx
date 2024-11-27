import React, {useState} from 'react';
import styled from 'styled-components';
import {useSelector} from 'react-redux';
import {RootState} from '../store';
// Debug logging utility
const DEBUG = true;
const log = (message: string, data?: any) => {
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
`;

interface InputAreaProps {
    onSendMessage: (message: string) => void;
}

const InputArea: React.FC<InputAreaProps> = ({onSendMessage}) => {
    log('Initializing component');
    const [message, setMessage] = useState('');
    const config = useSelector((state: RootState) => state.config);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        log('Submit attempt');
        if (message.trim()) {
            setIsSubmitting(true);
            log('Sending message', {
                messageLength: message.length,
                message: message.substring(0, 100) + (message.length > 100 ? '...' : '')
            });
            onSendMessage(message);
            setMessage('');
            setIsSubmitting(false);
            log('Message sent and form reset');
        } else {
            log('Empty message, not sending');
        }
    };

    const handleMessageChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const newMessage = e.target.value;
        log('Message changed', {
            length: newMessage.length,
            isEmpty: newMessage.trim().length === 0
        });
        setMessage(newMessage);
    };

    React.useEffect(() => {
        log('Component mounted', {configState: config});
        return () => {
            log('Component unmounting');
        };
    }, [config]);


    return (
        <InputContainer>
            <form onSubmit={handleSubmit}>
                <TextArea
                    value={message}
                    onChange={handleMessageChange}
                    placeholder="Type a message..."
                    rows={3}
                />
                <button type="submit">Send</button>
            </form>
        </InputContainer>
    );
};
// Log when module is imported
log('Module loaded');


export default InputArea;