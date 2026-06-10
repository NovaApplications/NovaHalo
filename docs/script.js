document.addEventListener("DOMContentLoaded", () => {
    // Points exactly to your hosted file directory path inside your GitHub repo
    // Date.now() builds a random string query on the end to instantly clear out stale CDN caches
    const jsonUrl = `docs/release.json?t=${Date.now()}`;
    const container = document.getElementById("releases-container");

    fetch(jsonUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // 1. Populate Core Header Metadata Elements
            document.getElementById("release-title").innerText = data.title;
            document.getElementById("release-date").innerText = `Released: ${data.releaseDate}`;
            document.getElementById("release-summary").innerText = data.summary;

            // 2. Populate Build Statistics Row
            document.getElementById("stat-version").innerText = `Build Code: ${data.versionCode} (prev: v${data.previousVersion})`;
            document.getElementById("stat-files").innerText = `📁 ${data.stats.filesChanged} files changed`;
            document.getElementById("stat-additions").innerText = `+${data.stats.additions} additions`;
            document.getElementById("stat-deletions").innerText = `-${data.stats.deletions} deletions`;

            // 3. Wipe out the loading placeholder text string
            container.innerHTML = "";

            // Mapping schema layout keys to design labels and Google brand colors
            const sectionMapping = [
                { key: "newFeatures", title: "🎨 What's New & Customization", color: "#4285f4" },
                { key: "optimizations", title: "🛠️ System Optimizations & Code Polish", color: "#f4b400" },
                { key: "bugFixes", title: "🐛 Bug Fixes & Stability", color: "#0f9d58" }
            ];

            // 4. Iterate through changelog collections to build clean layout cards
            sectionMapping.forEach(section => {
                const pointsArray = data.changelog[section.key];
                
                if (pointsArray && pointsArray.length > 0) {
                    // Create wrapper card component layout
                    const sectionCard = document.createElement("div");
                    sectionCard.style.background = "#ffffff";
                    sectionCard.style.border = "1px solid #e1e4e8";
                    sectionCard.style.borderRadius = "8px";
                    sectionCard.style.padding = "24px";
                    sectionCard.style.marginBottom = "25px";
                    sectionCard.style.boxShadow = "0 2px 5px rgba(0,0,0,0.03)";

                    // Styled subcategory label header string
                    const heading = document.createElement("h3");
                    heading.innerText = section.title;
                    heading.style.margin = "0 0 15px 0";
                    heading.style.fontSize = "1.35rem";
                    heading.style.color = "#222";
                    heading.style.borderLeft = `4px solid ${section.color}`;
                    heading.style.paddingLeft = "12px";
                    sectionCard.appendChild(heading);

                    // Unordered bullet listing structure
                    const bulletList = document.createElement("ul");
                    bulletList.style.margin = "0";
                    bulletList.style.paddingLeft = "20px";
                    bulletList.style.lineHeight = "1.7";
                    bulletList.style.color = "#333";

                    // Inject array item text configurations
                    pointsArray.forEach(text => {
                        const listItem = document.createElement("li");
                        listItem.style.marginBottom = "10px";
                        listItem.style.fontSize = "1.05rem";
                        
                        // Automatically bold the parameter descriptors before a colon
                        if (text.includes(":")) {
                            const parts = text.split(":");
                            listItem.innerHTML = `<strong style="color: #111;">${parts[0]}:</strong>${parts.slice(1).join(":")}`;
                        } else {
                            listItem.innerText = text;
                        }
                        
                        bulletList.appendChild(listItem);
                    });

                    sectionCard.appendChild(bulletList);
                    container.appendChild(sectionCard);
                }
            });
        })
        .catch(error => {
            console.error("Error processing version repository package metadata JSON:", error);
            container.innerHTML = `
                <div style="background: #fdf2f2; border: 1px solid #f8b4b4; padding: 15px; border-radius: 6px; color: #9b1c1c; font-family: sans-serif;">
                    <strong>Error Loading Updates:</strong> Unable to load live release details from release.json right now. Please check file placement paths and try again.
                </div>`;
        });
