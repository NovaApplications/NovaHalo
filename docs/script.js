document.addEventListener("DOMContentLoaded", () => {
    const jsonUrl = `releases.json?t=${Date.now()}`;
    const container = document.getElementById("releases-container");

    fetch(jsonUrl)
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }
            return response.json();
        })
        .then(releasesArray => {
            // Clear out the loading text completely
            container.innerHTML = "";

            // Loop through every single release entry inside the JSON file array list
            releasesArray.forEach(releaseData => {
                // Create a master container box wrapper for this specific version entry
                const releaseBox = document.createElement("div");
                releaseBox.className = "release-version-block";
                releaseBox.style.marginBottom = "50px";
                releaseBox.style.borderBottom = "1px dashed #ccc";
                releaseBox.style.paddingBottom = "30px";

                // Build out heading row components
                const titleHeading = document.createElement("h2");
                titleHeading.innerText = releaseData.title;
                titleHeading.style.fontSize = "1.8rem";
                titleHeading.style.margin = "0 0 5px 0";
                releaseBox.appendChild(titleHeading);

                const dateBadge = document.createElement("p");
                dateBadge.innerText = `Released on: ${releaseData.releaseDate} | Build Code: ${releaseData.versionCode}`;
                dateBadge.style.color = "#777";
                dateBadge.style.fontSize = "0.95rem";
                dateBadge.style.marginBottom = "15px";
                releaseBox.appendChild(dateBadge);

                const summaryText = document.createElement("p");
                summaryText.innerText = releaseData.summary;
                summaryText.style.fontSize = "1.1rem";
                summaryText.style.lineHeight = "1.6";
                summaryText.style.marginBottom = "25px";
                releaseBox.appendChild(summaryText);

                // Setup internal categories matching details
                const sectionMapping = [
                    { key: "newFeatures", title: "🎨 What's New & Customization" },
                    { key: "optimizations", title: "🛠️ System Optimizations & Code Polish" },
                    { key: "bugFixes", title: "🐛 Bug Fixes & Stability" }
                ];

                sectionMapping.forEach(section => {
                    const pointsArray = releaseData.changelog[section.key];
                    
                    if (pointsArray && pointsArray.length > 0) {
                        const heading = document.createElement("h3");
                        heading.innerText = section.title;
                        heading.style.marginTop = "20px";
                        heading.style.marginBottom = "10px";
                        heading.style.fontSize = "1.25rem";
                        releaseBox.appendChild(heading);

                        const bulletList = document.createElement("ul");
                        bulletList.style.paddingLeft = "20px";
                        bulletList.style.marginBottom = "20px";
                        bulletList.style.lineHeight = "1.7";

                        pointsArray.forEach(text => {
                            const listItem = document.createElement("li");
                            listItem.style.marginBottom = "6px";
                            
                            if (text.includes(":")) {
                                const parts = text.split(":");
                                listItem.innerHTML = `<strong>${parts[0]}:</strong>${parts.slice(1).join(":")}`;
                            } else {
                                listItem.innerText = text;
                            }
                            bulletList.appendChild(listItem);
                        });

                        releaseBox.appendChild(bulletList);
                    }
                });

                // Append the completed version segment into the core HTML container wrapper
                container.appendChild(releaseBox);
            });
        })
        .catch(error => {
            console.error("Error processing NovaOS project release logs history:", error);
            container.innerHTML = `<p style="color: red; font-style: italic;">Failed to parse history timeline stream variables.</p>`;
        });
});
