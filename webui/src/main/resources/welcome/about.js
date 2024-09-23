import {showModal} from './functions.js';

document.addEventListener('DOMContentLoaded', () => {
    const privacyLink = document.getElementById('privacy');
    const tosLink = document.getElementById('tos');
    if (privacyLink) {
        // Update the privacy link with the user's name and make it visible
        privacyLink.addEventListener('click', () => showModal('/privacy.html', false));
    }
    if (tosLink) {
        // Update the terms link with the user's name and make it visible
        tosLink.addEventListener('click', () => showModal('/tos.html', false));
    }
});