import {useDispatch} from 'react-redux';
import WebSocketService from '../services/websocket';
import {setModalContent, showModal as showModalAction} from '../store/slices/uiSlice';
import Prism from 'prismjs';

export const useModal = () => {
    const dispatch = useDispatch();

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
        const path = window.location.pathname;
        let url: string;
        if (endpoint.startsWith("/")) {
            url = `${protocol}//${host}:${port}${endpoint}`;
        } else {
            url = `${protocol}//${host}:${port}${path}${endpoint}`;
        }
        if (endpoint.endsWith("/")) {
            url = url + WebSocketService.getSessionId() + '/';
        } else {
            const separator = endpoint.includes('?') ? '&' : '?';
            url = url + separator + 'sessionId=' + WebSocketService.getSessionId();
        }
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

        dispatch(showModalAction(endpoint));
        dispatch(setModalContent('<div class="loading">Loading...</div>'));
        console.log('[Modal] Fetching content from:', getModalUrl(endpoint));

        fetch(getModalUrl(endpoint), {
            mode: 'cors',
            credentials: 'include',
            headers: {
                'Accept': 'text/html,application/json,*/*'
            }
        })
            .then(response => {
                console.log('[Modal] Received response:', {
                    status: response.status,
                    statusText: response.statusText
                });
                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }
                return response.text();
            })
            .then(content => {
                console.log('[Modal] Content received, length:', content.length);
                if (!content.trim()) {
                    throw new Error('Received empty content');
                }
                requestAnimationFrame(() => {
                    dispatch(setModalContent(content));
                    highlightCode();
                });
            })
            .catch(error => {
                console.error('[Modal] Failed to load content:', error);
                dispatch(setModalContent('<div class="error">Error loading content: ' + error.message + '</div>'));
                // Keep modal open to show error
            });
    };
    console.log('[Modal] Hook initialized');

    return {openModal, getModalUrl};
};