// Load releases from local JSON file instead of GitHub API
// This gives you full control over release notes

async function loadReleases() {
    const container = document.getElementById('releases-container');
    
    try {
        const response = await fetch('./releases.json');
        const data = await response.json();
        
        // Handle both single object and array formats
        const release = Array.isArray(data) ? data[0] : data;
        
        if (!response.ok || !release) {
            throw new Error('Failed to load releases');
        }
        
        const releaseHtml = `
            <div class="release-card">
                <div class="release-header">
                    <h3 class="release-title">${escapeHtml(release.title)}</h3>
                    <span class="release-tag latest">
                        ⭐ Latest - v${release.version}
                    </span>
                </div>
                <p class="release-date">Released on ${escapeHtml(release.releaseDate)}</p>
                <div class="release-body">
                    ${formatReleaseNotes(release.changelog, release.summary)}
                </div>
                <p style="font-size: 0.9rem; color: var(--text-light); margin-top: 1rem;">
                    📊 Changes: ${release.stats.filesChanged} files · +${release.stats.additions} · -${release.stats.deletions}
                </p>
                <a href="https://github.com/NovaApplications/NovaHalo/releases/tag/v${release.version}" target="_blank" class="release-link">View on GitHub →</a>
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
    const map = {
        '&': '&amp;',
        '<': '&lt;',
        '>': '&gt;',
        '"': '&quot;',
        "'": '&#039;'
    };
    return text.replace(/[&<>"']/g, m => map[m]);
}

// Format release notes from changelog object
function formatReleaseNotes(changelog, summary) {
    let html = '';
    
    if (summary) {
        html += `<p>${escapeHtml(summary)}</p>`;
    }
    
    if (changelog.newFeatures && changelog.newFeatures.length > 0) {
        html += '<h3>✨ New Features</h3><ul>';
        changelog.newFeatures.forEach(feature => {
            html += `<li>${escapeHtml(feature)}</li>`;
        });
        html += '</ul>';
    }
    
    if (changelog.optimizations && changelog.optimizations.length > 0) {
        html += '<h3>⚡ Optimizations</h3><ul>';
        changelog.optimizations.forEach(opt => {
            html += `<li>${escapeHtml(opt)}</li>`;
        });
        html += '</ul>';
    }
    
    if (changelog.bugFixes && changelog.bugFixes.length > 0) {
        html += '<h3>🐛 Bug Fixes</h3><ul>';
        changelog.bugFixes.forEach(fix => {
            html += `<li>${escapeHtml(fix)}</li>`;
        });
        html += '</ul>';
    }
    
    return html;
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
        margin: 1rem 0;
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