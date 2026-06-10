document.addEventListener("DOMContentLoaded", () => {
    // Path to your JSON file inside your repository structure
    // Date.now() works as a cache-buster to bypass the 10-minute GitHub Pages delay
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
            // 1. Populate metadata headers
            document.getElementById("release-title").innerText = `${data.title}`;
            document.getElementById("release-date").innerText = `Released: ${data.releaseDate}`;
            document.getElementById("release-summary").innerText = data.summary;

            // 2. Clear out the loading text placeholder
            container.innerHTML = "";

            // Mapping your customized JSON keys to clear headings
            const sectionMapping = [
                { key: "newFeatures", title: "🎨 What's New & Customization" },
                { key: "optimizations", title: "🛠️ System Optimizations & Code Polish" },
                { key: "bugFixes", title: "🐛 Bug Fixes & Stability" }
            ];

            // 3. Process each category array from the changelog
            sectionMapping.forEach(section => {
                const pointsArray = data.changelog[section.key];
                
                if (pointsArray && pointsArray.length > 0) {
                    // Create wrapper layout block
                    const sectionBlock = document.createElement("div");
                    sectionBlock.className = "changelog-section";
                    sectionBlock.style.marginBottom = "30px";

                    const heading = document.createElement("h3");
                    heading.innerText = section.title;
                    heading.style.fontSize = "1.3rem";
                    heading.style.marginBottom = "10px";
                    sectionBlock.appendChild(heading);

                    const bulletList = document.createElement("ul");
                    bulletList.className = "changelog-list";
                    bulletList.style.paddingLeft = "20px";
                    bulletList.style.lineHeight = "1.7";

                    // Inject individual bullet records
                    pointsArray.forEach(text => {
                        const listItem = document.createElement("li");
                        listItem.innerText = text;
                        listItem.style.marginBottom = "8px";
                        bulletList.appendChild(listItem);
                    });

                    sectionBlock.appendChild(bulletList);
                    container.appendChild(sectionBlock);
                }
            });
        })
        .catch(error => {
            console.error("Error fetching or parsing changelog data:", error);
            container.innerHTML = `<p class="error-text" style="color: red;">Failed to load update details. Please refresh or try again later.</p>`;
        });
});
