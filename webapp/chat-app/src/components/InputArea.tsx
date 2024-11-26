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

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        if (message.trim()) {
            onSendMessage(message);
            setMessage('');
        }
    };

    return (
        <InputContainer>
            <form onSubmit={handleSubmit}>
                <TextArea
                    value={message}
                    onChange={(e) => setMessage(e.target.value)}
                    placeholder="Type a message..."
                    rows={3}
                />
                <button type="submit">Send</button>
            </form>
        </InputContainer>
    );
};

export default InputArea;
