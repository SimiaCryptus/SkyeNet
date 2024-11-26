import {useDispatch, useSelector} from 'react-redux';
import {RootState} from '../store';
import WebSocketService from '../services/websocket';
import {showModal as showModalAction} from '../store/slices/uiSlice';
import Prism from 'prismjs';
import 'prismjs/themes/prism.css';
import 'prismjs/components/prism-javascript';
import 'prismjs/components/prism-typescript';
import 'prismjs/components/prism-jsx';
import 'prismjs/components/prism-tsx';
import {useState} from "react";

export const useModal = () => {
    const config = useSelector((state: RootState) => state.config.websocket);
    const dispatch = useDispatch();
    const [modalContent, setModalContent] = useState('');

    const getModalUrl = (endpoint: string) => {
        const protocol = window.location.protocol;
        const host = config.url || window.location.hostname;
        const port = config.port || window.location.port;
        // Handle endpoints that already have query parameters
        const separator = endpoint.includes('?') ? '&' : '?';
        return `${protocol}//${host}:${port}/${endpoint}${separator}sessionId=${WebSocketService.getSessionId()}`;
    };

    const openModal = (endpoint: string, event?: React.MouseEvent) => {
        if (event) {
            event.preventDefault();
            event.stopPropagation();
        }

        setModalContent('<div>Loading...</div>');
        dispatch(showModalAction(endpoint));

        fetch(getModalUrl(endpoint), {
            mode: 'cors',
            headers: {
                'Accept': 'text/html,application/json',
                credentials: 'include'
            }
        })
            .then(response => response.text())
            .then(content => {
                setModalContent(content);
                if (Prism) {
                    Prism.highlightAll();
                }
            })
            .catch(error => {
                console.error('[Modal] Failed to load content:', {
                    endpoint,
                    error: error.message,
                    status: error.status
                });
                setModalContent('<div>Error loading content. Please try again later.</div>');
                // Keep modal open to show error
            });
    };

    return {openModal, getModalUrl, modalContent};
};