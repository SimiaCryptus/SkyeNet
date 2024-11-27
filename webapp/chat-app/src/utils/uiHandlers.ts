import {store} from '../store';
import {toggleVerbose} from '../store/slices/uiSlice';
import {showModal} from '../store/slices/uiSlice';
import WebSocketService from '../services/websocket';

export const setupUIHandlers = () => {
    // Keyboard shortcuts
    document.addEventListener('keydown', (event) => {
        if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key.toLowerCase() === 'v') {
            event.preventDefault();
            store.dispatch(toggleVerbose());
        }
    });

    // Modal handlers
    document.addEventListener('click', (event) => {
        const target = event.target as HTMLElement;
        if (target.matches('[data-modal]')) {
            event.preventDefault();
            const modalType = target.getAttribute('data-modal');
            if (modalType) {
                store.dispatch(showModal(modalType));
            }
        }
    });

    // Message action handlers
    document.addEventListener('click', (event) => {
        const target = event.target as HTMLElement;
        const messageAction = target.getAttribute('data-message-action');
        const messageId = target.getAttribute('data-message-id');
        
        if (messageAction && messageId) {
            event.preventDefault();
            handleMessageAction(messageId, messageAction);
        }
    });
};

const handleMessageAction = (messageId: string, action: string) => {
    WebSocketService.send(`!${messageId},${action}`);
};