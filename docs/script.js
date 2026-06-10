document.addEventListener("DOMContentLoaded", () => {
    // FIXED PATH: Matches your exact filename 'releases.json' from your /docs folder
    // Date.now() bypasses the 10-minute GitHub Pages CDN cache freeze automatically
    const jsonUrl = `releases.json?t=${Date.now()}`;
    const container = document.getElementById("releases-container");

    fetch(jsonUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status} - Make sure releases.json exists.`);
            }
            return response.json();
        })
        .then(data => {
            // 1. Update the main heading text dynamically to match your version title
            const titleElement = document.getElementById("release-title");
            if (titleElement) {
                titleElement.innerText = data.title;
            }

            // 2. Clear out the "Loading..." placeholder text string
            container.innerHTML = "";

            // 3. Create a clean block for the summary text description
            const summaryPara = document.createElement("p");
            summaryPara.innerText = data.summary;
            summaryPara.style.marginBottom = "25px";
            summaryPara.style.fontSize = "1.1rem";
            summaryPara.style.lineHeight = "1.6";
            summaryPara.style.color = "inherit"; 
            container.appendChild(summaryPara);

            // Mapping your customized JSON sections to structural headings
            const sectionMapping = [
                { key: "newFeatures", title: "🎨 What's New & Customization" },
                { key: "optimizations", title: "🛠️ System Optimizations & Code Polish" },
                { key: "bugFixes", title: "🐛 Bug Fixes & Stability" }
            ];

            // 4. Loop through your changelog categories
            sectionMapping.forEach(section => {
                const pointsArray = data.changelog[section.key];
                
                if (pointsArray && pointsArray.length > 0) {
                    // Category Sub-header
                    const heading = document.createElement("h3");
                    heading.innerText = section.title;
                    heading.style.marginTop = "25px";
                    heading.style.marginBottom = "10px";
                    heading.style.fontSize = "1.3rem";
                    container.appendChild(heading);

                    // Standard Unordered Bullet List
                    const bulletList = document.createElement("ul");
                    bulletList.style.paddingLeft = "20px";
                    bulletList.style.marginBottom = "20px";
                    bulletList.style.lineHeight = "1.7";

                    // Inject individual lines
                    pointsArray.forEach(text => {
                        const listItem = document.createElement("li");
                        listItem.style.marginBottom = "8px";
                        listItem.style.fontSize = "1.05rem";
                        
                        // Automatically bold feature descriptors before a colon for crisp parsing
                        if (text.includes(":")) {
                            const parts = text.split(":");
                            listItem.innerHTML = `<strong>${parts[0]}:</strong>${parts.slice(1).join(":")}`;
                        } else {
                            listItem.innerText = text;
                        }
                        
                        bulletList.appendChild(listItem);
                    });

                    container.appendChild(bulletList);
                }
            });
        })
        .catch(error => {
            console.error("Error processing NovaOS project repository metadata JSON:", error);
            container.innerHTML = `
                <p class="error-text" style="color: red; font-style: italic; font-weight: bold;">
                    Failed to load latest NovaOS release notes. (Error: ${error.message})
                </p>`;
        });
});
