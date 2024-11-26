import React, {useState} from 'react';
import {useDispatch, useSelector} from 'react-redux';
import styled from 'styled-components';
import {RootState} from '../../store';
import {updateWebSocketConfig} from '../../store/slices/configSlice';

const MenuContainer = styled.div`
    padding: 1rem;
    background: ${({theme}) => theme.colors.surface};
    border-radius: ${({theme}) => theme.sizing.borderRadius.md};
    border: 1px solid ${({theme}) => theme.colors.border};
`;

const FormGroup = styled.div`
    margin-bottom: 1rem;
`;

const Label = styled.label`
    display: block;
    margin-bottom: 0.5rem;
    color: ${({theme}) => theme.colors.text.secondary};
`;

const Input = styled.input`
    width: 100%;
    padding: 0.5rem;
    border: 1px solid ${({theme}) => theme.colors.border};
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
    background: ${({theme}) => theme.colors.background};
    color: ${({theme}) => theme.colors.text.primary};
`;

const Button = styled.button`
    padding: 0.5rem 1rem;
    background: ${({theme}) => theme.colors.primary};
    color: white;
    border-radius: ${({theme}) => theme.sizing.borderRadius.sm};
    border: none;
    cursor: pointer;

    &:hover {
        opacity: 0.9;
    }
`;

export const WebSocketMenu: React.FC = () => {
    const dispatch = useDispatch();
    const wsConfig = useSelector((state: RootState) => state.config.websocket);

    const [config, setConfig] = useState({
        url: wsConfig.url,
        port: wsConfig.port,
        protocol: wsConfig.protocol
    });

    const handleSubmit = (e: React.FormEvent) => {
        e.preventDefault();
        console.log('Submitting WebSocket configuration:', config);
        dispatch(updateWebSocketConfig(config));
    };

    const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const {name, value} = e.target;
        setConfig(prev => ({...prev, [name]: value}));
    };

    return (
        <MenuContainer>
            <h3>WebSocket Configuration</h3>
            <form onSubmit={handleSubmit}>
                <FormGroup>
                    <Label htmlFor="protocol">Protocol</Label>
                    <Input
                        id="protocol"
                        name="protocol"
                        value={config.protocol}
                        onChange={handleChange}
                        placeholder="ws:// or wss://"
                    />
                </FormGroup>
                <FormGroup>
                    <Label htmlFor="url">Host URL</Label>
                    <Input
                        id="url"
                        name="url"
                        value={config.url}
                        onChange={handleChange}
                        placeholder="localhost or your server URL"
                    />
                </FormGroup>
                <FormGroup>
                    <Label htmlFor="port">Port</Label>
                    <Input
                        id="port"
                        name="port"
                        value={config.port}
                        onChange={handleChange}
                        placeholder="8080"
                    />
                </FormGroup>
                <Button type="submit">Save Configuration</Button>
            </form>
        </MenuContainer>
    );
};