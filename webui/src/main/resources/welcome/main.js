function showModal(endpoint) {
    fetchData(endpoint);
    document.getElementById('modal').style.display = 'block';
}

function closeModal() {
    document.getElementById('modal').style.display = 'none';
}

async function fetchData(endpoint) {
    try {
        // Add session id to the endpoint as a path parameter
        document.getElementById('modal-content').innerHTML = "<div>Loading...</div>";
        const response = await fetch(endpoint);
        const text = await response.text();
        document.getElementById('modal-content').innerHTML = "<div>" + text + "</div>";
    } catch (error) {
        console.error('Error fetching data:', error);
    }
}

function updateTabs() {
    const buttons = document.querySelectorAll('.tab-button');
    buttons.forEach(button => {
        button.addEventListener('click', () => {
            const forTab = button.getAttribute('data-for-tab');
            const tabsParent = button.parentElement.parentElement;
            const toShow = tabsParent.querySelector(`.tab-content[data-tab="${forTab}"]`);

            const allContents = tabsParent.querySelectorAll('.tab-content');
            // Remove active class from all tabs and contents
            // This line seems to be an error or misplaced, consider removing or fixing it
            buttons.forEach(btn => btn.classList.remove('active'));
            allContents.forEach(content => content.classList.remove('active'));

            // Add active class to clicked tab and corresponding content
            button.classList.add('active');
            toShow.classList.add('active');
        });
    });
}


document.addEventListener('DOMContentLoaded', () => {
    updateTabs();
    document.querySelector('.close').addEventListener('click', closeModal);

    window.addEventListener('click', (event) => {
        if (event.target === document.getElementById('modal')) {
            closeModal();
        }
    });

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

    // Get the privacy and terms links
    const privacyLink = document.getElementById('privacy');
    const tosLink = document.getElementById('tos');
    if (privacyLink) {
        // Update the privacy link with the user's name and make it visible
        privacyLink.addEventListener('click', () => showModal('/privacy.html'));
    }
    if (tosLink) {
        // Update the terms link with the user's name and make it visible
        tosLink.addEventListener('click', () => showModal('/tos.html'));
    }

    function setTheme(theme) {
        document.getElementById('theme_style').href = theme + '.css';
        localStorage.setItem('theme', theme);
    }
    const theme_normal = document.getElementById('theme_normal');
    if (theme_normal) {
        theme_normal.addEventListener('click', () => setTheme('main'));
    }
    const theme_night = document.getElementById('theme_night');
    if (theme_night) {
        theme_night.addEventListener('click', () => setTheme('night'));
    }
    const theme_forest = document.getElementById('theme_forest');
    if (theme_forest) {
        theme_forest.addEventListener('click', () => setTheme('forest'));
    }
    const theme_pony = document.getElementById('theme_pony');
    if (theme_pony) {
        theme_pony.addEventListener('click', () => setTheme('pony'));
    }
    const theme_alien = document.getElementById('theme_alien');
    if (theme_alien) {
        theme_alien.addEventListener('click', () => setTheme('alien'));
    }
    const savedTheme = localStorage.getItem('theme');
    if (savedTheme != null) {
        document.getElementById('theme_style').href = savedTheme + '.css';
    }

});
