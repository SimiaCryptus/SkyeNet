import {store} from '../store';
import {addMessage} from '../store/slices/messageSlice';
import {Message} from '../types';

export const setupMessageHandling = () => {
    const messageVersions = new Map<string, string>();
    const messageMap = new Map<string, string>();

    const handleMessage = (message: Message) => {
        const {id, version, content} = message;
        console.log(`[MessageHandler] Processing message: ${id} (v${version})`);
        
        messageVersions.set(id, version);
        messageMap.set(id, content);
        console.log(`[MessageHandler] Stored message content: "${content}"`);

        store.dispatch(addMessage(message));
        console.log(`[MessageHandler] Dispatched message to store`);
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
        console.warn(`[MessageSubstitution] Max depth (${MAX_DEPTH}) reached for content: "${messageContent}"`);
        return messageContent;
    }
    console.log(`[MessageSubstitution] Processing substitutions at depth ${depth}: "${messageContent}"`);


    return messageContent.replace(/\{([^}]+)}/g, (match, id) => {
        console.log(`[MessageSubstitution] Found reference: ${id}`);
        const substitution = messageMap.get(id);
        if (substitution) {
            console.log(`[MessageSubstitution] Substituting ${id} with: "${substitution}"`);
            return substituteMessages(substitution, messageMap, depth + 1);
        }
        console.log(`[MessageSubstitution] No substitution found for: ${id}`);
        return match;
    });
};