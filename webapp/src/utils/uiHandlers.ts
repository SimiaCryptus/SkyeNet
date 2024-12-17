import {store} from '../store';
import {showModal, toggleVerbose} from '../store/slices/uiSlice';
import WebSocketService from '../services/websocket';
import {debounce} from './tabHandling';

export const setupUIHandlers = () => {
    console.log('Setting up UI event handlers...');

    // Create debounced handler outside event listener
    const handleKeyboardShortcut = debounce((event: KeyboardEvent) => {
        if ((event.ctrlKey || event.metaKey) && event.shiftKey && event.key === 'V') {
            event.preventDefault();
            console.log('Keyboard shortcut triggered: Toggle verbose mode');
            store.dispatch(toggleVerbose());
        }
    }, 250);

    // Keyboard shortcuts
    document.addEventListener('keydown', handleKeyboardShortcut);
    // Cleanup function to remove event listeners
    return () => {
        document.removeEventListener('keydown', handleKeyboardShortcut);
    };

    // Modal handlers
    document.addEventListener('click', (event) => {
        const target = event.target as HTMLElement;
        if (target.matches('[data-modal]')) {
            event.preventDefault();
            const modalType = target.getAttribute('data-modal');
            if (modalType) {
                console.log(`Modal trigger clicked: ${modalType}`);
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
            console.log(`Message action triggered - ID: ${messageId}, Action: ${messageAction}`);
            handleMessageAction(messageId, messageAction);
        }
    });

    console.log('UI event handlers setup complete');
};

const handleMessageAction = (messageId: string, action: string) => {
    console.log(`Sending message action to WebSocket - ID: ${messageId}, Action: ${action}`);
    WebSocketService.send(`!${messageId},${action}`);
};