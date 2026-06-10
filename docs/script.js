document.addEventListener("DOMContentLoaded", () => {
    // Assumes release.json is in the exact same folder level as index.html
    // Date.now() builds a dynamic query parameters string to force-bypass the 10-minute CDN freeze
    const jsonUrl = `release.json?t=${Date.now()}`;
    const container = document.getElementById("releases-container");

    fetch(jsonUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(data => {
            // 1. Update the main heading text dynamically to match your version title
            document.getElementById("release-title").innerText = data.title;

            // 2. Clear out the "Loading..." placeholder text string
            container.innerHTML = "";

            // Create a clean block for the summary text description
            const summaryPara = document.createElement("p");
            summaryPara.innerText = data.summary;
            summaryPara.style.marginBottom = "25px";
            summaryPara.style.fontSize = "1.1rem";
            summaryPara.style.color = "inherit"; 
            container.appendChild(summaryPara);

            // Mapping your customized JSON sections to structural headings
            const sectionMapping = [
                { key: "newFeatures", title: "🎨 What's New & Customization" },
                { key: "optimizations", title: "🛠️ System Optimizations & Code Polish" },
                { key: "bugFixes", title: "🐛 Bug Fixes & Stability" }
            ];

            // 3. Loop through your changelog categories
            sectionMapping.forEach(section => {
                const pointsArray = data.changelog[section.key];
                
                if (pointsArray && pointsArray.length > 0) {
                    // Category Sub-header
                    const heading = document.createElement("h3");
                    heading.innerText = section.title;
                    heading.style.marginTop = "20px";
                    heading.style.marginBottom = "10px";
                    container.appendChild(heading);

                    // Standard Unordered Bullet List
                    const bulletList = document.createElement("ul");
                    bulletList.style.paddingLeft = "20px";
                    bulletList.style.marginBottom = "20px";

                    // Inject individual lines
                    pointsArray.forEach(text => {
                        const listItem = document.createElement("li");
                        listItem.style.marginBottom = "8px";
                        
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
                <p class="error-text" style="color: red; font-style: italic;">
                    Failed to load latest NovaOS release notes. Please ensure release.json exists in the same folder as this page.
                </p>`;
        });
});
