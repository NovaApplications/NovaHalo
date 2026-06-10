// Load README content and release notes from JSON file

async function loadReadme() {
    const readmeContainer = document.getElementById('readme-content');
    
    try {
        const response = await fetch('../README.md');
        const markdown = await response.text();
        
        if (!response.ok) {
            throw new Error('Failed to load README');
        }
        
        // Simple markdown to HTML conversion
        let html = markdown
            .replace(/^### (.*?)$/gm, '<h3>$1</h3>')
            .replace(/^## (.*?)$/gm, '<h3>$1</h3>')
            .replace(/^# (.*?)$/gm, '<h2>$1</h2>')
            .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
            .replace(/__(.*?)__/g, '<strong>$1</strong>')
            .replace(/\*(.*?)\*/g, '<em>$1</em>')
            .replace(/_(.*?)_/g, '<em>$1</em>')
            .replace(/\n\n/g, '</p><p>')
            .replace(/\n/g, '<br>');
        
        html = '<p>' + html + '</p>';
        html = html.replace(/<p><\/p>/g, '');
        
        readmeContainer.innerHTML = html;
    } catch (error) {
        console.error('Error loading README:', error);
        readmeContainer.innerHTML = '<p>Unable to load project information.</p>';
    }
}

async function loadReleases() {
    const container = document.getElementById('releases-container');
    
    try {
        const response = await fetch('./releases.json');
        const data = await response.json();
        
        // The JSON should be a single object or array with a single object
        const release = Array.isArray(data) ? data[0] : data;
        
        if (!response.ok || !release || !release.title) {
            throw new Error('Failed to load releases');
        }
        
        // Build the release card HTML
        let releaseBody = '';
        
        // Add summary if available
        if (release.summary) {
            releaseBody += `<p><strong>Summary:</strong> ${escapeHtml(release.summary)}</p>`;
        }
        
        // Add features
        if (release.changelog && release.changelog.newFeatures && release.changelog.newFeatures.length > 0) {
            releaseBody += '<h3>✨ New Features</h3><ul>';
            release.changelog.newFeatures.forEach(feature => {
                releaseBody += `<li>${escapeHtml(feature)}</li>`;
            });
            releaseBody += '</ul>';
        }
        
        // Add optimizations
        if (release.changelog && release.changelog.optimizations && release.changelog.optimizations.length > 0) {
            releaseBody += '<h3>⚡ Optimizations</h3><ul>';
            release.changelog.optimizations.forEach(opt => {
                releaseBody += `<li>${escapeHtml(opt)}</li>`;
            });
            releaseBody += '</ul>';
        }
        
        // Add bug fixes
        if (release.changelog && release.changelog.bugFixes && release.changelog.bugFixes.length > 0) {
            releaseBody += '<h3>🐛 Bug Fixes</h3><ul>';
            release.changelog.bugFixes.forEach(fix => {
                releaseBody += `<li>${escapeHtml(fix)}</li>`;
            });
            releaseBody += '</ul>';
        }
        
        let statsHtml = '';
        if (release.stats) {
            statsHtml = `
                <div class="stats">
                    <div class="stat-item">📊 <strong>${release.stats.filesChanged}</strong> files changed</div>
                    <div class="stat-item"><span class="stat-additions">+${release.stats.additions}</span></div>
                    <div class="stat-item"><span class="stat-deletions">-${release.stats.deletions}</span></div>
                </div>
            `;
        }
        
        const releaseHtml = `
            <div class="release-card">
                <div class="release-header">
                    <h3 class="release-title">${escapeHtml(release.title)}</h3>
                    <span class="release-tag latest">
                        ⭐ Latest - v${escapeHtml(release.version)}
                    </span>
                </div>
                <p class="release-date">Released on ${escapeHtml(release.releaseDate)}</p>
                <div class="release-body">
                    ${releaseBody}
                </div>
                ${statsHtml}
                <a href="https://github.com/NovaApplications/NovaHalo/releases/tag/v${escapeHtml(release.version)}" target="_blank" class="release-link">View on GitHub →</a>
            </div>
        `;
        
        container.innerHTML = releaseHtml;
    } catch (error) {
        console.error('Error loading releases:', error);
        container.innerHTML = `
            <p class="loading">Unable to load releases at this moment.</p>
            <p class="loading" style="font-size: 0.9rem;">Visit <a href="https://github.com/NovaApplications/NovaHalo/releases" target="_blank" style="color: var(--primary-color);">GitHub Releases</a> to view them directly.</p>
        `;
    }
}

// Simple HTML escape function
function escapeHtml(text) {
    if (!text) return '';
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return String(text).replace(/[&<>"']/g, m => map[m]);
}

// Load both when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        loadReadme();
        loadReleases();
    });
} else {
    loadReadme();
    loadReleases();
}