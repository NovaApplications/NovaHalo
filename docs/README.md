# Nova Halo Documentation

This directory contains the GitHub Pages site for Nova Halo. The site features:

- **Modern, responsive design** with clean UI components
- **Automatic release notes integration** that pulls directly from GitHub releases
- **Mobile-friendly layout** that works on all devices
- **Easy to customize** - all styling is in `styles.css`

## Files

- `index.html` - Main page with hero section, features, releases, and about
- `styles.css` - All styling and responsive design
- `script.js` - JavaScript for fetching and displaying releases from GitHub API
- `_config.yml` - Jekyll configuration for GitHub Pages

## Adding Release Notes

Release notes are automatically fetched from your GitHub repository's [Releases](https://github.com/NovaApplications/NovaHalo/releases) page. To add a new release:

1. Go to [Releases](https://github.com/NovaApplications/NovaHalo/releases)
2. Click "Create a new release"
3. Add a tag name (e.g., `v1.0.0`)
4. Add a title and description in Markdown format
5. Click "Publish release"

The release will automatically appear on the website within minutes!

## Customization

### Colors

Edit the CSS variables in `styles.css` to change the color scheme:

```css
:root {
    --primary-color: #2196F3;      /* Main blue color */
    --secondary-color: #1976D2;    /* Darker blue */
    --text-dark: #212121;          /* Dark text */
    --text-light: #757575;         /* Light gray text */
    --bg-light: #f5f5f5;           /* Light background */
    --border-color: #e0e0e0;       /* Border color */
    --success-color: #4CAF50;      /* Green for success */
}
```

### Content

Edit `index.html` to:
- Change the navigation menu
- Update feature cards
- Add more sections
- Modify the about text

## Deployment

GitHub Pages automatically deploys the `docs/` folder from the `main` branch. Every push to `main` will update the site.

## Viewing Locally

To preview the site locally:

```bash
# Install dependencies (if you have Jekyll installed)
jekyll serve --source docs

# Then visit http://localhost:4000
```

Or simply open `index.html` in your browser for a basic preview (release notes may not load locally due to CORS).
