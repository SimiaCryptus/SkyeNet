document.addEventListener('DOMContentLoaded', () => {
    function setTheme(theme) {
        document.getElementById('theme_style').href = '/' + theme + '.css';
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
        document.getElementById('theme_style').href = '/' + savedTheme + '.css';
    }
});
