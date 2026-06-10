// Fetch and display GitHub releases
const REPO = 'NovaApplications/NovaHalo';
const API_URL = `https://api.github.com/repos/${REPO}/releases`;

async function loadReleases() {
    const container = document.getElementById('releases-container');
    
    try {
        const response = await fetch(API_URL);
        const releases = await response.json();
        
        if (!response.ok) {
            throw new Error('Failed to fetch releases');
        }
        
        if (releases.length === 0) {
            container.innerHTML = '<p class="loading">No releases yet. Check back soon!</p>';
            return;
        }
        
        container.innerHTML = releases.map((release, index) => `
            <div class="release-card">
                <div class="release-header">
                    <h3 class="release-title">${escapeHtml(release.name || release.tag_name)}</h3>
                    <span class="release-tag ${index === 0 ? 'latest' : ''}">
                        ${index === 0 ? '⭐ Latest' : release.tag_name}
                    </span>
                </div>
                <p class="release-date">Released on ${new Date(release.published_at).toLocaleDateString('en-US', { year: 'numeric', month: 'long', day: 'numeric' })}</p>
                <div class="release-body">
                    ${release.body ? markdownToHtml(release.body) : '<p><em>No description provided</em></p>'}
                </div>
                <a href="${release.html_url}" target="_blank" class="release-link">View on GitHub →</a>
            </div>
        `).join('');
    } catch (error) {
        console.error('Error loading releases:', error);
        container.innerHTML = `
            <p class="loading">Unable to load releases at this moment.</p>
            <p class="loading" style="font-size: 0.9rem;">Visit <a href="https://github.com/${REPO}/releases" target="_blank" style="color: var(--primary-color);">GitHub Releases</a> to view them directly.</p>
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

// Simple markdown to HTML converter for release notes
function markdownToHtml(markdown) {
    let html = escapeHtml(markdown);
    
    // Headers
    html = html.replace(/^### (.*?)$/gm, '<h4>$1</h4>');
    html = html.replace(/^## (.*?)$/gm, '<h3>$1</h3>');
    html = html.replace(/^# (.*?)$/gm, '<h2>$1</h2>');
    
    // Bold
    html = html.replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/__(.+?)__/g, '<strong>$1</strong>');
    
    // Italic
    html = html.replace(/\*(.*?)\*/g, '<em>$1</em>');
    html = html.replace(/_(.+?)_/g, '<em>$1</em>');
    
    // Code blocks
    html = html.replace(/```([\s\S]*?)```/g, '<pre><code>$1</code></pre>');
    
    // Inline code
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');
    
    // Unordered lists
    html = html.replace(/^\* (.+)$/gm, '<li>$1</li>');
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    html = html.replace(/(<li>.*?<\/li>)/s, '<ul>$1</ul>');
    
    // Ordered lists
    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');
    
    // Line breaks
    html = html.replace(/\n\n/g, '</p><p>');
    html = html.replace(/\n/g, '<br>');
    
    // Wrap in paragraphs if not already in a block element
    html = '<p>' + html + '</p>';
    html = html.replace(/<p><\/p>/g, '');
    html = html.replace(/<p>(<h[1-4])/g, '$1');
    html = html.replace(/(<\/h[1-4]>)<\/p>/g, '$1');
    html = html.replace(/<p>(<ul|<ol)/g, '$1');
    html = html.replace(/(<\/ul>|<\/ol>)<\/p>/g, '$1');
    html = html.replace(/<p>(<pre)/g, '$1');
    html = html.replace(/(<\/pre>)<\/p>/g, '$1');
    
    return html;
}

// Add CSS for generated HTML elements
const style = document.createElement('style');
style.textContent = `
    .release-body h2 {
        font-size: 1.5rem;
        margin-top: 1.5rem;
        margin-bottom: 1rem;
        color: var(--primary-color);
    }
    .release-body h3 {
        font-size: 1.2rem;
        margin-top: 1rem;
        margin-bottom: 0.5rem;
        color: var(--primary-color);
    }
    .release-body h4 {
        font-size: 1rem;
        margin-top: 0.8rem;
        margin-bottom: 0.5rem;
        color: var(--text-dark);
    }
    .release-body code {
        background: var(--bg-light);
        padding: 2px 6px;
        border-radius: 3px;
        font-family: 'Courier New', monospace;
        font-size: 0.9em;
    }
    .release-body pre {
        background: var(--bg-light);
        padding: 1rem;
        border-radius: 6px;
        overflow-x: auto;
        margin: 1rem 0;
    }
    .release-body pre code {
        background: none;
        padding: 0;
    }
    .release-body ul, .release-body ol {
        margin: 1rem 0 1rem 1.5rem;
    }
    .release-body li {
        margin-bottom: 0.5rem;
    }
    .release-body strong {
        font-weight: 600;
        color: var(--text-dark);
    }
`;
document.head.appendChild(style);

// Load releases when page loads
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', loadReleases);
} else {
    loadReleases();
}