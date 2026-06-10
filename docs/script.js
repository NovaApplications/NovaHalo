// Load releases from local JSON file
// This gives you full control over release notes

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
                ${release.stats ? `
                    <p style="font-size: 0.85rem; color: var(--text-light); margin-top: 1rem; padding-top: 1rem; border-top: 1px solid var(--border-color);">
                        📊 <strong>Changes:</strong> ${release.stats.filesChanged} files changed · <span style="color: var(--success-color);">+${release.stats.additions}</span> · <span style="color: #f44336;">-${release.stats.deletions}</span>
                    </p>
                ` : ''}
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

// Add CSS for generated HTML elements
const style = document.createElement('style');
style.textContent = `
    .release-body h3 {
        font-size: 1.2rem;
        margin-top: 1.5rem;
        margin-bottom: 0.8rem;
        color: var(--primary-color);
    }
    .release-body h3:first-child {
        margin-top: 0;
    }
    .release-body ul {
        margin: 0.5rem 0 1rem 1.5rem;
        list-style-type: disc;
    }
    .release-body li {
        margin-bottom: 0.7rem;
        line-height: 1.6;
    }
    .release-body p {
        margin: 0.8rem 0;
        line-height: 1.8;
    }
`;
document.head.appendChild(style);

// Load releases when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadReleases);
} else {
    loadReleases();
}