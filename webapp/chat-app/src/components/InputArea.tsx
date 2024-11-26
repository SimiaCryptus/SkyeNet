import React, {useState} from 'react';
import styled from 'styled-components';

const InputContainer = styled.div`
    padding: 1rem;
    background-color: ${(props) => props.theme.colors.surface};
    border-top: 1px solid ${(props) => props.theme.colors.border};
`;

const TextArea = styled.textarea`
    width: 100%;
    padding: 0.5rem;
    border-radius: ${(props) => props.theme.sizing.borderRadius.md};
    border: 1px solid ${(props) => props.theme.colors.border};
    font-family: inherit;
    resize: vertical;
`;

interface InputAreaProps {
    onSendMessage: (message: string) => void;
}

const InputArea: React.FC<InputAreaProps> = ({onSendMessage}) => {
    const [message, setMessage] = useState('');
    console.log('[InputArea] Rendering with message:', message);

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('[InputArea] Form submitted with message:', message);
        if (message.trim()) {
            console.log('[InputArea] Sending message:', message);
            onSendMessage(message);
            setMessage('');
            console.log('[InputArea] Message sent and input cleared');
        } else {
            console.log('[InputArea] Empty message, not sending');
        }
    };
    const handleMessageChange = (e: React.ChangeEvent<HTMLTextAreaElement>) => {
        const newMessage = e.target.value;
        console.log('[InputArea] Message changed:', newMessage);
        setMessage(newMessage);
    };


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
// Log when component is imported
console.log('[InputArea] Component loaded');


export default InputArea;