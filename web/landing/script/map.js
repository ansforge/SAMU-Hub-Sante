document.getElementById("div-map").addEventListener("click", (e) => {
    const dep = e.target.closest(".department");
    if (!dep) return;
    document.getElementById("departements-list").value = dep.dataset.numDep;
    onDepartmentSelected(dep);
});

document.getElementById("dep-sel-btn").addEventListener("click", () => {
    const selectedDepValue = document.getElementById("departements-list").value;
    if (selectedDepValue === "") {
        unselectDepartment();
        hideInfoSelectedDepartment();
    } else {
        const dep = document.querySelector(`[data-num-dep="${selectedDepValue}"]`);
        onDepartmentSelected(dep);
    }
});

function onDepartmentSelected(dep) {
    unselectDepartment();
    document
        .querySelectorAll(`.department[data-num-dep='${dep.dataset.numDep}']`)
        .forEach((d) => d.classList.add("selected"));
    renderDepartmentInfo(dep);
}

function unselectDepartment() {
    document
        .querySelectorAll(".department.selected")
        .forEach((d) => d.classList.remove("selected"));
}

function hideInfoSelectedDepartment() {
    const divInfo = document.getElementById("department-infos");
    if (divInfo.classList.contains("d-block")) {
        divInfo.classList.replace("d-block", "d-none");
    }
}

function renderDepartmentInfo(dep) {
    const divInfo = document.getElementById("department-infos");
    divInfo.innerHTML = "";
    if (divInfo.classList.contains("d-none")) {
        divInfo.classList.replace("d-none", "d-block");
    }

    // Title
    divInfo.appendChild(createTitleH2(dep));

    // Content
    if (!dep.classList.contains("prod")) {
        divInfo.style.backgroundColor = "var(--gray-500)";
        divInfo.appendChild(
            createParagraph(
                "Aucune information disponible pour ce département en environnement de production.",
            ),
        );
    } else {
        divInfo.style.backgroundColor = "var(--primary)";
        divInfo.appendChild(createParagraph(dep.dataset.lien));
    }
}

function createTitleH2(dep) {
    const title = document.createElement("h2");
    title.innerText = dep.querySelector("title").innerHTML;
    return title;
}

function createParagraph(text) {
    const p = document.createElement("p");
    p.innerText = text;
    return p;
}