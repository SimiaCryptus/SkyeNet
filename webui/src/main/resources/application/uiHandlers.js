import {closeModal, findAncestor, getSessionId, showModal, toggleVerbose} from './functions.js';
import {queueMessage} from "./chat.js";

function isCtrlOrCmd(event) {
    return event.ctrlKey || (navigator.platform.toLowerCase().indexOf('mac') !== -1 && event.metaKey);
}

function addClickHandler(elementId, action) {
    const element = document.getElementById(elementId);
    if (element) element.addEventListener('click', action);
}


export function setupUIHandlers() {
    addClickHandler('history', () => showModal('sessions'));
    addClickHandler('settings', () => showModal('settings'));
    addClickHandler('usage', () => showModal('usage'));
    addClickHandler('verbose', toggleVerbose);
    addClickHandler('delete', () => showModal('delete'));
    addClickHandler('cancel', () => showModal('cancel'));
    addClickHandler('threads', () => showModal('threads'));
    const shareElement = document.getElementById('share');
    if (shareElement) {
        shareElement.addEventListener('click', () => showModal('share?url=' + encodeURIComponent(window.location.href) + "&loadImages=true", false));
    }
    const closeElement = document.querySelector('.close');
    if (closeElement) closeElement.addEventListener('click', closeModal);

    document.addEventListener('click', (event) => {
        const modal = document.getElementById('modal');
        if (event.target === modal) {
            closeModal();
        }
    });

    document.body.addEventListener('click', handleBodyClick);

    let filesElement = document.getElementById("files");
    if (filesElement) filesElement.addEventListener("click", handleFilesClick);
    // Add keyboard event listener for toggling verbose mode
    document.addEventListener('keydown', (event) => {
        if (isCtrlOrCmd(event) && event.shiftKey && event.key.toLowerCase() === 'v') {
            event.preventDefault();
            toggleVerbose();
            console.log('Verbose mode toggled via hotkey');
        }
    });
}

function handleBodyClick(event) {
    const target = event.target;
    console.log('Click event on body, target:', target);
    const hrefLink = findAncestor(target, '.href-link');
    if (hrefLink) {
        const messageId = hrefLink.getAttribute('data-id');
        console.log('Href link clicked, messageId:', messageId);
        handleMessageAction(messageId, 'link');
    } else {
        const playButton = findAncestor(target, '.play-button');
        if (playButton) {
            const messageId = playButton.getAttribute('data-id');
            console.log('Play button clicked, messageId:', messageId);
            handleMessageAction(messageId, 'run');
        } else {
            const regenButton = findAncestor(target, '.regen-button');
            if (regenButton) {
                const messageId = regenButton.getAttribute('data-id');
                console.log('Regen button clicked, messageId:', messageId);
                handleMessageAction(messageId, 'regen');
            } else {
                const cancelButton = findAncestor(target, '.cancel-button');
                if (cancelButton) {
                    const messageId = cancelButton.getAttribute('data-id');
                    console.log('Cancel button clicked, messageId:', messageId);
                    handleMessageAction(messageId, 'stop');
                } else {
                    const textSubmitButton = findAncestor(target, '.text-submit-button');
                    if (textSubmitButton) {
                        const messageId = textSubmitButton.getAttribute('data-id');
                        console.log('Text submit button clicked, messageId:', messageId);
                        const text = document.querySelector('.reply-input[data-id="' + messageId + '"]').value;
                        const escapedText = encodeURIComponent(text);
                        handleMessageAction(messageId, `userTxt,${escapedText}`);
                    }
                }
            }
        }
    }
}

function handleFilesClick(event) {
    event.preventDefault();
    console.log('Files element clicked');
    const sessionId = getSessionId();
    const url = "fileIndex/" + sessionId + "/";
    window.open(url, "_blank");
}

function handleMessageAction(messageId, action) {
    queueMessage('!' + messageId + ',' + action);
}