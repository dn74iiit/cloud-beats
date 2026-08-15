document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('download-form');
    const urlInput = document.getElementById('spotify-url');
    const submitBtn = document.getElementById('download-btn');
    const btnText = document.querySelector('.btn-text');
    const loader = document.querySelector('.loader');
    const statusContainer = document.getElementById('status-container');
    const statusMessage = document.getElementById('status-message');

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const url = urlInput.value.trim();
        if (!url) return;

        // Set Loading State
        submitBtn.disabled = true;
        btnText.classList.add('hidden');
        loader.classList.remove('hidden');
        statusContainer.classList.add('hidden');

        try {
            const response = await fetch('/api/download', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ url: url })
            });

            const data = await response.json();

            showStatus(data.message, data.success ? 'success' : 'error');
            if (data.success) {
                urlInput.value = ''; // clear input on success
            }
        } catch (error) {
            showStatus('A network error occurred. Please try again.', 'error');
        } finally {
            // Reset Button State
            submitBtn.disabled = false;
            btnText.classList.remove('hidden');
            loader.classList.add('hidden');
        }
    });

    function showStatus(message, type) {
        statusMessage.textContent = message;
        statusContainer.className = `status-container status-${type}`;
        statusContainer.classList.remove('hidden');
    }
});
