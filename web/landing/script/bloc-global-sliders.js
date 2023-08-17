function sliderControlsObserver() {
    let e = document.querySelectorAll(".tns-controls");
    const t = new MutationObserver((function (e, t) {
        for (const t of e)
            "attributes" === t.type && "style" === t.attributeName && sliderToggleHeaderAlign()
    }
    ));
    for (const n of e)
        t.observe(n, {
            attributes: !0
        })
}
function sliderToggleHeaderAlign() {
    let e = document.querySelectorAll(".tns-controls");
    if (e.length > 0) {
        let t, n, r = ":hidden";
        for (let o = 0; o < 2; o++)
            n = $(e).filter(r),
                t = n.closest(".container").children(".paragraph__header"),
                ":hidden" === r ? t.hasClass("justify-content-center") || (t.addClass("justify-content-center"),
                    t.children().addClass("text-center")) : ":visible" === r && t.hasClass("justify-content-center") && (t.removeClass("justify-content-center"),
                        t.children().removeClass("text-center")),
                r = ":visible"
    }
}
$(document).ready((function () {
    sliderControlsObserver(),
        sliderToggleHeaderAlign()
}
));
var fn_slider = function (e, t, n, r, o, s, l = 30) {
    var i = Array.from(document.querySelectorAll(e));
    void 0 !== i && null != i && i.forEach((function (e) {
        var i = e.querySelector(t);
        if (i.children.length > 1)
            e = tns({
                container: i,
                items: 1,
                gutter: l,
                loop: !1,
                nav: !1,
                controlsPosition: n,
                controlsText: ["élément précédent", "élément suivant"],
                responsive: {
                    576: {
                        items: r
                    },
                    992: {
                        items: o
                    },
                    1200: {
                        items: s
                    }
                }
            })
    }
    ))
};
