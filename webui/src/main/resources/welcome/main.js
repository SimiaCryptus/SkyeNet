function showModal(endpoint) {
    fetchData(endpoint);
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function fetchData(endpoint) {
    try {
        const response = await fetch(endpoint);
        const text = await response.text();
        document.getElementById('modal-content').innerHTML = "<div>" + text + "</div>";
        Prism.highlightAll();
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

document.addEventListener('DOMContentLoaded', () => {

    window.addEventListener('click', (event) => {
        if (event.target === document.getElementById('modal')) {
            closeModal();
        }
    });
    document.querySelector('.close').addEventListener('click', closeModal);

    // Get the login and username links
    const loginLink = document.getElementById('login');
    const usernameLink = document.getElementById('username');
    const userSettingsLink = document.getElementById('user-settings');
    const userUsageLink = document.getElementById('user-usage');
    const logoutLink = document.getElementById('logout');

    // Fetch user information
    fetch('userInfo')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.name) {
                // Update the username link with the user's name and make it visible
                usernameLink.textContent = data.name;
                usernameLink.style = 'visibility: visible';

                // Update the href for user settings and make it visible
                userSettingsLink.addEventListener('click', () => showModal('/userSettings'));
                userSettingsLink.style = 'visibility: visible';

                // Update the href for user usage and make it visible
                userUsageLink.addEventListener('click', () => showModal('/usage'));
                userUsageLink.style = 'visibility: visible';

                // Update the logout link and make it visible
                logoutLink.href = '/logout';
                logoutLink.style = 'visibility: visible';

                // Hide the login link since the user is logged in
                loginLink.style = 'visibility: hidden';
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });

});

