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
        console.log('[Modal] Constructing modal URL for endpoint:', endpoint);
        const protocol = window.location.protocol;
        const host = config.url || window.location.hostname;
        const port = config.port || window.location.port;
        // Handle endpoints that already have query parameters
        const separator = endpoint.includes('?') ? '&' : '?';
        const url = `${protocol}//${host}:${port}/${endpoint}${separator}sessionId=${WebSocketService.getSessionId()}`;
        console.log('[Modal] Constructed URL:', url);
        return url;
    };

    const openModal = (endpoint: string, event?: React.MouseEvent) => {
        console.log('[Modal] Opening modal for endpoint:', endpoint);
        if (event) {
            console.log('[Modal] Preventing default event behavior');
            event.preventDefault();
            event.stopPropagation();
        }
        console.log('[Modal] Setting initial loading state');

        setModalContent('<div>Loading...</div>');
        dispatch(showModalAction(endpoint));
        console.log('[Modal] Fetching content from:', getModalUrl(endpoint));

        fetch(getModalUrl(endpoint), {
            mode: 'cors',
            headers: {
                'Accept': 'text/html,application/json',
                credentials: 'include'
            }
        })
            .then(response => {
                console.log('[Modal] Received response:', {
                    status: response.status,
                    statusText: response.statusText
                });
                return response.text();
            })
            .then(content => {
                console.log('[Modal] Content received, length:', content.length);
                setModalContent(content);
                if (Prism) {
                    console.log('[Modal] Applying Prism syntax highlighting');
                    Prism.highlightAll();
                }
            })
            .catch(error => {
                console.error('[Modal] Failed to load content:', {
                    endpoint,
                    error: error.message,
                    status: error.status,
                    stack: error.stack
                });
                setModalContent('<div>Error loading content. Please try again later.</div>');
                // Keep modal open to show error
            });
    };
    console.log('[Modal] Hook initialized');

    return {openModal, getModalUrl, modalContent};
};