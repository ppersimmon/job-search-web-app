document.addEventListener('DOMContentLoaded', function() {
    const hamburger = document.getElementById('hamburgerMenu');
    const navLinks = document.getElementById('navLinks');

    if (hamburger && navLinks) {
        hamburger.addEventListener('click', function() {
            navLinks.classList.toggle('active');
        });
    }

    const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content');
    const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content');

    const saveButtons = document.querySelectorAll('.btn-save');
    saveButtons.forEach(button => {
        button.addEventListener('click', function(e) {
            e.preventDefault();

            const vacancyId = this.getAttribute('data-id');
            if (!vacancyId) return;

            fetch('/account/favorites/toggle', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/x-www-form-urlencoded',
                    [csrfHeader]: csrfToken
                },
                body: new URLSearchParams({
                    'vacancyId': vacancyId
                })
            })
                .then(response => response.text())
                .then(result => {
                    if (result === 'unauthorized') {
                        window.location.href = '/login';
                    } else if (result === 'added') {
                        this.classList.add('active');
                    } else if (result === 'removed') {
                        this.classList.remove('active');

                        if (window.location.pathname === '/account') {
                            const card = this.closest('.card');
                            if (card) {
                                card.style.display = 'none';
                            }
                        }
                    }
                })
                .catch(error => console.error('Помилка виконання запиту:', error));
        });
    });
});