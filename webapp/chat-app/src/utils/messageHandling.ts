import {store} from '../store';
import {addMessage} from '../store/slices/messageSlice';
import {Message} from '../types';
import {updateTabs} from './tabHandling';
import WebSocketService from '../services/websocket';
import {logger} from './logger';

export const handleMessageAction = (messageId: string, action: string) => {
    logger.debug('Processing message action', {messageId, action});
    
    // Handle text submit actions specially
    if (action === 'text-submit') {
        const input = document.querySelector(`.reply-input[data-message-id="${messageId}"]`) as HTMLTextAreaElement;
        if (input) {
            const text = input.value;
            const escapedText = encodeURIComponent(text);
            const message = `!${messageId},userTxt,${escapedText}`;
            WebSocketService.send(message);
            logger.debug('Sent text submit message', {messageId, text: text.substring(0, 100)});
            input.value = '';
        }
        return;
    }
    // Handle link clicks
    if (action === 'link') {
        logger.debug('Processing link click', {messageId});
        WebSocketService.send(`!${messageId},link`);
        return;
    }
    // Handle run/play button clicks
    if (action === 'run') {
        logger.debug('Processing run action', {messageId}); 
        WebSocketService.send(`!${messageId},run`);
        return;
    }
    // Handle regenerate button clicks
    if (action === 'regen') {
        logger.debug('Processing regenerate action', {messageId});
        WebSocketService.send(`!${messageId},regen`);
        return;
    }
    // Handle cancel button clicks 
    if (action === 'stop') {
        logger.debug('Processing stop action', {messageId});
        WebSocketService.send(`!${messageId},stop`);
        return;
    }
    // Handle all other actions
    logger.debug('Processing generic action', {messageId, action});
    WebSocketService.send(`!${messageId},${action}`);
};

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
        // Process tabs after message is added
        if (message.isHtml) {
            requestAnimationFrame(() => {
                updateTabs();
            });
        }
        // Process tabs after message is added
        if (message.isHtml) {
            requestAnimationFrame(() => {
                updateTabs();
            });
        }
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