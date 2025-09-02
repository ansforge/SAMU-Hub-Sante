$(document).ready(function () {
  const e = document.querySelector("#region-infos"),
    i = document.querySelector("#region-title"),
    t = document.querySelector("#region-desc"),
    o = document.querySelector(".region-link");
  let n;
  null != o && (n = document.querySelector(".region-link").innerHTML);
  const a = document.querySelector("#region-sel-btn");
  null != a &&
    a.addEventListener("click", function () {
      l(document.getElementById("regions-list").value);
    });
  var c = {
    "FR-GF": {
      name: "Guyane",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Guyane", value: "#Guyane" },
    },
    "FR-H": {
      name: "Corse",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Corse", value: "#Corse" },
    },
    "FR-F": {
      name: "Centre-Val de Loire",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Centre-Val de Loire", value: "#Centre-Val de Loire" },
    },
    "FR-E": {
      name: "Bretagne",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Bretagne", value: "#Bretagne" },
    },
    "FR-X1": {
      name: "Bourgogne-Franche-Comté",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: {
        label: "Bourgogne-Franche-Comté",
        value: "#Bourgogne-Franche-Comté",
      },
    },
    "FR-MQ": {
      name: "Martinique",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Martinique", value: "#Martinique" },
    },
    "FR-YT": {
      name: "Mayotte",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Mayotte", value: "#Mayotte" },
    },
    "FR-X4": {
      name: "Grand Est",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Grand Est", value: "#Grand Est" },
    },
    "FR-X5": {
      name: "Occitanie",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Occitanie", value: "#Occitanie" },
    },
    "FR-X6": {
      name: "Hauts-de-France",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Hauts-de-France", value: "#Hauts-de-France" },
    },
    "FR-X7": {
      name: "Auvergne-Rhône-Alpes",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Auvergne-Rhône-Alpes", value: "#Auvergne-Rhône-Alpes" },
    },
    "FR-X3": {
      name: "Normandie",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Normandie", value: "#Normandie" },
    },
    "FR-R": {
      name: "Pays de la Loire",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Pays de la Loire", value: "#Pays de la Loire" },
    },
    "FR-GP": {
      name: "Guadeloupe",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Guadeloupe", value: "#Guadeloupe" },
    },
    "FR-U": {
      name: "Provence-Alpes-Côte-d'Azur",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: {
        label: "Provence-Alpes-Côte-d'Azur",
        value: "#Provence-Alpes-Côte-d'Azur",
      },
    },
    "FR-J": {
      name: "Île-de-France",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Île-de-France", value: "#Île-de-France" },
    },
    "FR-X2": {
      name: "Nouvelle-Aquitaine",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Nouvelle-Aquitaine", value: "#Nouvelle-Aquitaine" },
    },
    "FR-RE": {
      name: "Réunion",
      description:
        "Lorem ipsum dolor sit amet consectetur adipisicing elit. Ipsum fugiat adipisicing sit anim ullamco proident. Do consequat velit culpa ipsum et minim proident eiusmod consectetur tempor occaecat minim dolore.",
      link: { label: "Réunion", value: "#Réunion" },
    },
  };
  function l(a) {
    (e.classList.contains("d-none") && e.classList.replace("d-none", "d-block"),
      (i.innerText = c[a].name),
      (t.innerText = c[a].description),
      (o.innerHTML = n + c[a].link.label),
      (o.href = c[a].link.value));
    var l = $("#map").vectorMap("get", "mapObject");
    (l.clearSelectedRegions(), l.setSelectedRegions(a));
  }
  ("undefined" != typeof drupalSettings &&
    void 0 !== drupalSettings.asip &&
    void 0 !== drupalSettings.asip.map &&
    (c = drupalSettings.asip.map),
    $(function () {
      $("#map").vectorMap({
        map: "fr_regions_2016_merc",
        backgroundColor: "#ffffff",
        zoomOnScrollSpeed: 1,
        regionsSelectable: !0,
        regionsSelectableOne: !0,
        regionStyle: {
          initial: {
            fill: "#DEE0EA",
            "fill-opacity": 1,
            stroke: "none",
            "stroke-width": 0,
            "stroke-opacity": 1,
          },
          hover: { fill: "#C5C8DA", "fill-opacity": 1, cursor: "pointer" },
          selected: { fill: "#1D71B8" },
          selectedHover: {},
        },
        onRegionClick: function (e, i) {
          (l(i),
            (document.getElementById("regions-list").value = i),
            document
              .getElementById("region-sel-btn")
              .removeAttribute("disabled"));
        },
      });
    }),
    $("#regions-list")
      .on("change", function () {
        $("#region-sel-btn").prop("disabled", !$(this).val());
      })
      .trigger("change"));
});
