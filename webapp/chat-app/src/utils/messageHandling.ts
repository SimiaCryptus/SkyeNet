import {store} from '../store';
import {addMessage} from '../store/slices/messageSlice';
import {Message} from '../types';

export const setupMessageHandling = () => {
    const messageVersions = new Map<string, string>();
    const messageMap = new Map<string, string>();

    const handleMessage = (message: Message) => {
        const {id, version, content} = message;
        
        messageVersions.set(id, version);
        messageMap.set(id, content);

        store.dispatch(addMessage(message));
    };

    return {
        handleMessage,
        messageVersions,
        messageMap
    };
};

export const substituteMessages = (
    messageContent: string, 
    messageMap: Map<string, string>,
    depth = 0
) : string => {
    const MAX_DEPTH = 10;
    if (depth > MAX_DEPTH) {
        console.warn('Max substitution depth reached');
        return messageContent;
    }

    return messageContent.replace(/\{([^}]+)}/g, (match, id) => {
        const substitution = messageMap.get(id);
        if (substitution) {
            return substituteMessages(substitution, messageMap, depth + 1);
        }
        return match;
    });
};