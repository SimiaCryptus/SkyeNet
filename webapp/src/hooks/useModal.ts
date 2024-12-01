import {useDispatch, useSelector} from 'react-redux';
import {RootState} from '../store';
import WebSocketService from '../services/websocket';
import {showModal as showModalAction} from '../store/slices/uiSlice';
import {useState} from "react";
import Prism from 'prismjs';

export const useModal = () => {
    const dispatch = useDispatch();
    const [modalContent, setModalContent] = useState('');

    // Helper to highlight code blocks
    const highlightCode = () => {
        if (typeof window !== 'undefined') {
            requestAnimationFrame(() => {
                const modalElement = document.querySelector('.modal-content');
                if (modalElement) {
                    Prism.highlightAllUnder(modalElement);
                }
            });
        }
    };

    const getModalUrl = (endpoint: string) => {
        console.log('[Modal] Constructing modal URL for endpoint:', endpoint);
        const protocol = window.location.protocol;
        const host = window.location.hostname;
        const port = window.location.port;
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
                // Highlight code after content is set
                requestAnimationFrame(() => {
                    highlightCode();
                });
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