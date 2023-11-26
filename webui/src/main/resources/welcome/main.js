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

    const loginLink = document.getElementById('username');
    if (loginLink) {
        loginLink.href = '/login?redirect=' + encodeURIComponent(window.location.pathname);
    }

    fetch('userInfo')
        .then(response => {
            if (!response.ok) {
                throw new Error('Network response was not ok');
            }
            return response.json();
        })
        .then(data => {
            if (data.name && loginLink) {
                loginLink.innerHTML = data.name;
                loginLink.href = "javascript:void(0);";
                loginLink.addEventListener('click', () => showModal('/userSettings'));
            }
        })
        .catch(error => {
            console.error('There was a problem with the fetch operation:', error);
        });
});

