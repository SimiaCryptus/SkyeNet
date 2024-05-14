function updateTabs() {
    document.querySelectorAll('.tab-button').forEach(button => {
        button.addEventListener('click', (event) => { // Ensure the event is passed as a parameter
            event.stopPropagation();
            const forTab = button.getAttribute('data-for-tab');
            let tabsParent = button.closest('.tabs-container');
            tabsParent.querySelectorAll('.tab-button').forEach(tabButton => tabButton.classList.remove('active'));
            button.classList.add('active');
            tabsParent.querySelectorAll('.tab-content').forEach(content => {
                const contentParent = content.closest('.tabs-container');
                if (contentParent === tabsParent) {
                    if (content.getAttribute('data-tab') === forTab) {
                        content.classList.add('active');
                    } else if (content.classList.contains('active')) {
                        content.classList.remove('active')
                    }
                }
            });
        })
    });
}

document.addEventListener('DOMContentLoaded', () => {
    try {
        updateTabs();
    } catch (e) {
        console.log("Error updating tabs: " + e);
    }
});

window.updateTabs = updateTabs; // Expose updateTabs to the global scope