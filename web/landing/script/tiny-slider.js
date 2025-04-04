/*!
	Tiny slider - v2.9.2
	https://github.com/ganlanyuan/tiny-slider

	***
	* AW modifications:
	*	- Added class and span.btn-text for prev/next buttons
	*	- Changed text of tns-liveregion
	***
 */
var tns = function () {
	var t = window
		, e = t.requestAnimationFrame || t.webkitRequestAnimationFrame || t.mozRequestAnimationFrame || t.msRequestAnimationFrame || function (t) {
			return setTimeout(t, 16)
		}
		, n = window
		, i = n.cancelAnimationFrame || n.mozCancelAnimationFrame || function (t) {
			clearTimeout(t)
		}
		;
	function a() {
		for (var t, e, n, i = arguments[0] || {}, a = 1, r = arguments.length; a < r; a++)
			if (null !== (t = arguments[a]))
				for (e in t)
					i !== (n = t[e]) && void 0 !== n && (i[e] = n);
		return i
	}
	function r(t) {
		return 0 <= ["true", "false"].indexOf(t) ? JSON.parse(t) : t
	}
	function o(t, e, n, i) {
		if (i)
			try {
				t.setItem(e, n)
			} catch (t) { }
		return n
	}
	function u() {
		var t = document
			, e = t.body;
		return e || ((e = t.createElement("body")).fake = !0),
			e
	}
	var l = document.documentElement;
	function s(t) {
		var e = "";
		return t.fake && (e = l.style.overflow,
			t.style.background = "",
			t.style.overflow = l.style.overflow = "hidden",
			l.appendChild(t)),
			e
	}
	function c(t, e) {
		t.fake && (t.remove(),
			l.style.overflow = e,
			l.offsetHeight)
	}
	function f(t, e, n, i) {
		"insertRule" in t ? t.insertRule(e + "{" + n + "}", i) : t.addRule(e, n, i)
	}
	function d(t) {
		return ("insertRule" in t ? t.cssRules : t.rules).length
	}
	function v(t, e, n) {
		for (var i = 0, a = t.length; i < a; i++)
			e.call(n, t[i], i)
	}
	var p = "classList" in document.createElement("_")
		, h = p ? function (t, e) {
			return t.classList.contains(e)
		}
			: function (t, e) {
				return 0 <= t.className.indexOf(e)
			}
		, m = p ? function (t, e) {
			h(t, e) || t.classList.add(e)
		}
			: function (t, e) {
				h(t, e) || (t.className += " " + e)
			}
		, y = p ? function (t, e) {
			h(t, e) && t.classList.remove(e)
		}
			: function (t, e) {
				h(t, e) && (t.className = t.className.replace(e, ""))
			}
		;
	function g(t, e) {
		return t.hasAttribute(e)
	}
	function b(t, e) {
		return t.getAttribute(e)
	}
	function x(t) {
		return void 0 !== t.item
	}
	function C(t, e) {
		if (t = x(t) || t instanceof Array ? t : [t],
			"[object Object]" === Object.prototype.toString.call(e))
			for (var n = t.length; n--;)
				for (var i in e)
					t[n].setAttribute(i, e[i])
	}
	function w(t, e) {
		t = x(t) || t instanceof Array ? t : [t];
		for (var n = (e = e instanceof Array ? e : [e]).length, i = t.length; i--;)
			for (var a = n; a--;)
				t[i].removeAttribute(e[a])
	}
	function M(t) {
		for (var e = [], n = 0, i = t.length; n < i; n++)
			e.push(t[n]);
		return e
	}
	function T(t, e) {
		"none" !== t.style.display && (t.style.display = "none")
	}
	function E(t, e) {
		"none" === t.style.display && (t.style.display = "")
	}
	function N(t) {
		return "none" !== window.getComputedStyle(t).display
	}
	function A(t) {
		if ("string" == typeof t) {
			var e = [t]
				, n = t.charAt(0).toUpperCase() + t.substr(1);
			["Webkit", "Moz", "ms", "O"].forEach((function (i) {
				"ms" === i && "transform" !== t || e.push(i + n)
			}
			)),
				t = e
		}
		for (var i = document.createElement("fakeelement"), a = (t.length,
			0); a < t.length; a++) {
			var r = t[a];
			if (void 0 !== i.style[r])
				return r
		}
		return !1
	}
	function L(t, e) {
		var n = !1;
		return /^Webkit/.test(t) ? n = "webkit" + e + "End" : /^O/.test(t) ? n = "o" + e + "End" : t && (n = e.toLowerCase() + "end"),
			n
	}
	var B = !1;
	try {
		var S = Object.defineProperty({}, "passive", {
			get: function () {
				B = !0
			}
		});
		window.addEventListener("test", null, S)
	} catch (t) { }
	var H = !!B && {
		passive: !0
	};
	function O(t, e, n) {
		for (var i in e) {
			var a = 0 <= ["touchstart", "touchmove"].indexOf(i) && !n && H;
			t.addEventListener(i, e[i], a)
		}
	}
	function D(t, e) {
		for (var n in e) {
			var i = 0 <= ["touchstart", "touchmove"].indexOf(n) && H;
			t.removeEventListener(n, e[n], i)
		}
	}
	function k() {
		return {
			topics: {},
			on: function (t, e) {
				this.topics[t] = this.topics[t] || [],
					this.topics[t].push(e)
			},
			off: function (t, e) {
				if (this.topics[t])
					for (var n = 0; n < this.topics[t].length; n++)
						if (this.topics[t][n] === e) {
							this.topics[t].splice(n, 1);
							break
						}
			},
			emit: function (t, e) {
				e.type = t,
					this.topics[t] && this.topics[t].forEach((function (n) {
						n(e, t)
					}
					))
			}
		}
	}
	Object.keys || (Object.keys = function (t) {
		var e = [];
		for (var n in t)
			Object.prototype.hasOwnProperty.call(t, n) && e.push(n);
		return e
	}
	),
		"remove" in Element.prototype || (Element.prototype.remove = function () {
			this.parentNode && this.parentNode.removeChild(this)
		}
		);
	var R = function (t) {
		t = a({
			container: ".slider",
			mode: "carousel",
			axis: "horizontal",
			items: 1,
			gutter: 0,
			edgePadding: 0,
			fixedWidth: !1,
			autoWidth: !1,
			viewportMax: !1,
			slideBy: 1,
			center: !1,
			controls: !0,
			controlsPosition: "top",
			controlsText: ["prev", "next"],
			controlsContainer: !1,
			prevButton: !1,
			nextButton: !1,
			nav: !0,
			navPosition: "top",
			navContainer: !1,
			navAsThumbnails: !1,
			arrowKeys: !1,
			speed: 300,
			autoplay: !1,
			autoplayPosition: "top",
			autoplayTimeout: 5e3,
			autoplayDirection: "forward",
			autoplayText: ["start", "stop"],
			autoplayHoverPause: !1,
			autoplayButton: !1,
			autoplayButtonOutput: !0,
			autoplayResetOnVisibility: !0,
			animateIn: "tns-fadeIn",
			animateOut: "tns-fadeOut",
			animateNormal: "tns-normal",
			animateDelay: !1,
			loop: !0,
			rewind: !1,
			autoHeight: !1,
			responsive: !1,
			lazyload: !1,
			lazyloadSelector: ".tns-lazy-img",
			touch: !0,
			mouseDrag: !1,
			swipeAngle: 15,
			nested: !1,
			preventActionWhenRunning: !1,
			preventScrollOnTouch: !1,
			freezable: !0,
			onInit: !1,
			useLocalStorage: !0
		}, t || {});
		var n = document
			, l = window
			, p = {
				ENTER: 13,
				SPACE: 32,
				LEFT: 37,
				RIGHT: 39
			}
			, x = {}
			, B = t.useLocalStorage;
		if (B) {
			var S = navigator.userAgent
				, H = new Date;
			try {
				(x = l.localStorage) ? (x.setItem(H, H),
					B = x.getItem(H) == H,
					x.removeItem(H)) : B = !1,
					B || (x = {})
			} catch (S) {
				B = !1
			}
			B && (x.tnsApp && x.tnsApp !== S && ["tC", "tPL", "tMQ", "tTf", "t3D", "tTDu", "tTDe", "tADu", "tADe", "tTE", "tAE"].forEach((function (t) {
				x.removeItem(t)
			}
			)),
				localStorage.tnsApp = S)
		}
		var I, P, z, W, q, F, j, V = x.tC ? r(x.tC) : o(x, "tC", function () {
			var t = document
				, e = u()
				, n = s(e)
				, i = t.createElement("div")
				, a = !1;
			e.appendChild(i);
			try {
				for (var r, o = "(10px * 10)", l = ["calc" + o, "-moz-calc" + o, "-webkit-calc" + o], f = 0; f < 3; f++)
					if (r = l[f],
						i.style.width = r,
						100 === i.offsetWidth) {
						a = r.replace(o, "");
						break
					}
			} catch (t) { }
			return e.fake ? c(e, n) : i.remove(),
				a
		}(), B), G = x.tPL ? r(x.tPL) : o(x, "tPL", function () {
			var t, e = document, n = u(), i = s(n), a = e.createElement("div"), r = e.createElement("div"), o = "";
			a.className = "tns-t-subp2",
				r.className = "tns-t-ct";
			for (var l = 0; l < 70; l++)
				o += "<div></div>";
			return r.innerHTML = o,
				a.appendChild(r),
				n.appendChild(a),
				t = Math.abs(a.getBoundingClientRect().left - r.children[67].getBoundingClientRect().left) < 2,
				n.fake ? c(n, i) : a.remove(),
				t
		}(), B), Q = x.tMQ ? r(x.tMQ) : o(x, "tMQ", (P = document,
			W = s(z = u()),
			q = P.createElement("div"),
			j = "@media all and (min-width:1px){.tns-mq-test{position:absolute}}",
			(F = P.createElement("style")).type = "text/css",
			q.className = "tns-mq-test",
			z.appendChild(F),
			z.appendChild(q),
			F.styleSheet ? F.styleSheet.cssText = j : F.appendChild(P.createTextNode(j)),
			I = window.getComputedStyle ? window.getComputedStyle(q).position : q.currentStyle.position,
			z.fake ? c(z, W) : q.remove(),
			"absolute" === I), B), X = x.tTf ? r(x.tTf) : o(x, "tTf", A("transform"), B), Y = x.t3D ? r(x.t3D) : o(x, "t3D", function (t) {
				if (!t)
					return !1;
				if (!window.getComputedStyle)
					return !1;
				var e, n = document, i = u(), a = s(i), r = n.createElement("p"), o = 9 < t.length ? "-" + t.slice(0, -9).toLowerCase() + "-" : "";
				return o += "transform",
					i.insertBefore(r, null),
					r.style[t] = "translate3d(1px,1px,1px)",
					e = window.getComputedStyle(r).getPropertyValue(o),
					i.fake ? c(i, a) : r.remove(),
					void 0 !== e && 0 < e.length && "none" !== e
			}(X), B), K = x.tTDu ? r(x.tTDu) : o(x, "tTDu", A("transitionDuration"), B), J = x.tTDe ? r(x.tTDe) : o(x, "tTDe", A("transitionDelay"), B), U = x.tADu ? r(x.tADu) : o(x, "tADu", A("animationDuration"), B), _ = x.tADe ? r(x.tADe) : o(x, "tADe", A("animationDelay"), B), Z = x.tTE ? r(x.tTE) : o(x, "tTE", L(K, "Transition"), B), $ = x.tAE ? r(x.tAE) : o(x, "tAE", L(U, "Animation"), B), tt = l.console && "function" == typeof l.console.warn, et = ["container", "controlsContainer", "prevButton", "nextButton", "navContainer", "autoplayButton"], nt = {};
		if (et.forEach((function (e) {
			if ("string" == typeof t[e]) {
				var i = t[e]
					, a = n.querySelector(i);
				if (nt[e] = i,
					!a || !a.nodeName)
					return void (tt && console.warn("Can't find", t[e]));
				t[e] = a
			}
		}
		)),
			!(t.container.children.length < 1)) {
			var it = t.responsive
				, at = t.nested
				, rt = "carousel" === t.mode;
			if (it) {
				0 in it && (t = a(t, it[0]),
					delete it[0]);
				var ot = {};
				for (var ut in it) {
					var lt = it[ut];
					lt = "number" == typeof lt ? {
						items: lt
					} : lt,
						ot[ut] = lt
				}
				it = ot,
					ot = null
			}
			if (rt || function t(e) {
				for (var n in e)
					rt || ("slideBy" === n && (e[n] = "page"),
						"edgePadding" === n && (e[n] = !1),
						"autoHeight" === n && (e[n] = !1)),
						"responsive" === n && t(e[n])
			}(t),
				!rt) {
				t.axis = "horizontal",
					t.slideBy = "page",
					t.edgePadding = !1;
				var st = t.animateIn
					, ct = t.animateOut
					, ft = t.animateDelay
					, dt = t.animateNormal
			}
			var vt, pt, ht = "horizontal" === t.axis, mt = n.createElement("div"), yt = n.createElement("div"), gt = t.container, bt = gt.parentNode, xt = gt.outerHTML, Ct = gt.children, wt = Ct.length, Mt = Pn(), Tt = !1;
			it && ii(),
				rt && (gt.className += " tns-vpfix");
			var Et, Nt, At, Lt, Bt, St, Ht, Ot = t.autoWidth, Dt = Fn("fixedWidth"), kt = Fn("edgePadding"), Rt = Fn("gutter"), It = Wn(), Pt = Fn("center"), zt = Ot ? 1 : Math.floor(Fn("items")), Wt = Fn("slideBy"), qt = t.viewportMax || t.fixedWidthViewportWidth, Ft = Fn("arrowKeys"), jt = Fn("speed"), Vt = t.rewind, Gt = !Vt && t.loop, Qt = Fn("autoHeight"), Xt = Fn("controls"), Yt = Fn("controlsText"), Kt = Fn("nav"), Jt = Fn("touch"), Ut = Fn("mouseDrag"), _t = Fn("autoplay"), Zt = Fn("autoplayTimeout"), $t = Fn("autoplayText"), te = Fn("autoplayHoverPause"), ee = Fn("autoplayResetOnVisibility"), ne = (Ht = document.createElement("style"),
				document.querySelector("head").appendChild(Ht),
				Ht.sheet ? Ht.sheet : Ht.styleSheet), ie = t.lazyload, ae = t.lazyloadSelector, re = [], oe = Gt ? (Bt = function () {
					if (Ot || Dt && !qt)
						return wt - 1;
					var e = Dt ? "fixedWidth" : "items"
						, n = [];
					if ((Dt || t[e] < wt) && n.push(t[e]),
						it)
						for (var i in it) {
							var a = it[i][e];
							a && (Dt || a < wt) && n.push(a)
						}
					return n.length || n.push(0),
						Math.ceil(Dt ? qt / Math.min.apply(null, n) : Math.max.apply(null, n))
				}(),
					St = rt ? Math.ceil((5 * Bt - wt) / 2) : 4 * Bt - wt,
					St = Math.max(Bt, St),
					qn("edgePadding") ? St + 1 : St) : 0, ue = rt ? wt + 2 * oe : wt + oe, le = !(!Dt && !Ot || Gt), se = Dt ? Bi() : null, ce = !rt || !Gt, fe = ht ? "left" : "top", de = "", ve = "", pe = Dt ? function () {
						return Pt && !Gt ? wt - 1 : Math.ceil(-se / (Dt + Rt))
					}
						: Ot ? function () {
							for (var t = ue; t--;)
								if (Et[t] >= -se)
									return t
						}
							: function () {
								return Pt && rt && !Gt ? wt - 1 : Gt || rt ? Math.max(0, ue - Math.ceil(zt)) : ue - 1
							}
				, he = kn(Fn("startIndex")), me = he, ye = (Dn(),
					0), ge = Ot ? null : pe(), be = t.preventActionWhenRunning, xe = t.swipeAngle, Ce = !xe || "?", we = !1, Me = t.onInit, Te = new k, Ee = " tns-slider tns-" + t.mode, Ne = gt.id || (Lt = window.tnsId,
						window.tnsId = Lt ? Lt + 1 : 1,
						"tns" + window.tnsId), Ae = Fn("disable"), Le = !1, Be = t.freezable, Se = !(!Be || Ot) && ni(), He = !1, Oe = {
							click: zi,
							keydown: function (t) {
								t = Xi(t);
								var e = [p.LEFT, p.RIGHT].indexOf(t.keyCode);
								0 <= e && (0 === e ? $e.disabled || zi(t, -1) : tn.disabled || zi(t, 1))
							}
						}, De = {
							click: function (t) {
								if (we) {
									if (be)
										return;
									Ii()
								}
								for (var e = Yi(t = Xi(t)); e !== rn && !g(e, "data-nav");)
									e = e.parentNode;
								if (g(e, "data-nav")) {
									var n = sn = Number(b(e, "data-nav"))
										, i = Dt || Ot ? n * wt / un : n * zt;
									Pi(Fe ? n : Math.min(Math.ceil(i), wt - 1), t),
										cn === n && (mn && Vi(),
											sn = -1)
								}
							},
							keydown: function (t) {
								t = Xi(t);
								var e = n.activeElement;
								if (g(e, "data-nav")) {
									var i = [p.LEFT, p.RIGHT, p.ENTER, p.SPACE].indexOf(t.keyCode)
										, a = Number(b(e, "data-nav"));
									0 <= i && (0 === i ? 0 < a && Qi(an[a - 1]) : 1 === i ? a < un - 1 && Qi(an[a + 1]) : Pi(sn = a, t))
								}
							}
						}, ke = {
							mouseover: function () {
								mn && (qi(),
									yn = !0)
							},
							mouseout: function () {
								yn && (Wi(),
									yn = !1)
							}
						}, Re = {
							visibilitychange: function () {
								n.hidden ? mn && (qi(),
									bn = !0) : bn && (Wi(),
										bn = !1)
							}
						}, Ie = {
							keydown: function (t) {
								t = Xi(t);
								var e = [p.LEFT, p.RIGHT].indexOf(t.keyCode);
								0 <= e && zi(t, 0 === e ? -1 : 1)
							}
						}, Pe = {
							touchstart: _i,
							touchmove: Zi,
							touchend: $i,
							touchcancel: $i
						}, ze = {
							mousedown: _i,
							mousemove: Zi,
							mouseup: $i,
							mouseleave: $i
						}, We = qn("controls"), qe = qn("nav"), Fe = !!Ot || t.navAsThumbnails, je = qn("autoplay"), Ve = qn("touch"), Ge = qn("mouseDrag"), Qe = "tns-slide-active", Xe = "tns-complete", Ye = {
							load: function (t) {
								di(Yi(t))
							},
							error: function (t) {
								var e;
								e = Yi(t),
									m(e, "failed"),
									vi(e)
							}
						}, Ke = "force" === t.preventScrollOnTouch;
			if (We)
				var Je, Ue, _e = t.controlsContainer, Ze = t.controlsContainer ? t.controlsContainer.outerHTML : "", $e = t.prevButton, tn = t.nextButton, en = t.prevButton ? t.prevButton.outerHTML : "", nn = t.nextButton ? t.nextButton.outerHTML : "";
			if (qe)
				var an, rn = t.navContainer, on = t.navContainer ? t.navContainer.outerHTML : "", un = Ot ? wt : ea(), ln = 0, sn = -1, cn = In(), fn = cn, dn = "tns-nav-active", vn = "Carousel Page ", pn = " (Current Slide)";
			if (je)
				var hn, mn, yn, gn, bn, xn = "forward" === t.autoplayDirection ? 1 : -1, Cn = t.autoplayButton, wn = t.autoplayButton ? t.autoplayButton.outerHTML : "", Mn = ["<span class='tns-visually-hidden'>", " animation</span>"];
			if (Ve || Ge)
				var Tn, En, Nn = {}, An = {}, Ln = !1, Bn = ht ? function (t, e) {
					return t.x - e.x
				}
					: function (t, e) {
						return t.y - e.y
					}
					;
			Ot || On(Ae || Se),
				X && (fe = X,
					de = "translate",
					Y ? (de += ht ? "3d(" : "3d(0px, ",
						ve = ht ? ", 0px, 0px)" : ", 0px)") : (de += ht ? "X(" : "Y(",
							ve = ")")),
				rt && (gt.className = gt.className.replace("tns-vpfix", "")),
				function () {
					(qn("gutter"),
						mt.className = "tns-outer",
						yt.className = "tns-inner",
						mt.id = Ne + "-ow",
						yt.id = Ne + "-iw",
						"" === gt.id && (gt.id = Ne),
						Ee += G || Ot ? " tns-subpixel" : " tns-no-subpixel",
						Ee += V ? " tns-calc" : " tns-no-calc",
						Ot && (Ee += " tns-autowidth"),
						Ee += " tns-" + t.axis,
						gt.className += Ee,
						rt ? ((vt = n.createElement("div")).id = Ne + "-mw",
							vt.className = "tns-ovh",
							mt.appendChild(vt),
							vt.appendChild(yt)) : mt.appendChild(yt),
						Qt) && ((vt || yt).className += " tns-ah");
					if (bt.insertBefore(mt, gt),
						yt.appendChild(gt),
						v(Ct, (function (t, e) {
							m(t, "tns-item"),
								t.id || (t.id = Ne + "-item" + e),
								!rt && dt && m(t, dt),
								C(t, {
									"aria-hidden": "true",
									tabindex: "-1"
								})
						}
						)),
						oe) {
						for (var e = n.createDocumentFragment(), i = n.createDocumentFragment(), a = oe; a--;) {
							var r = a % wt
								, o = Ct[r].cloneNode(!0);
							if (w(o, "id"),
								i.insertBefore(o, i.firstChild),
								rt) {
								var u = Ct[wt - 1 - r].cloneNode(!0);
								w(u, "id"),
									e.appendChild(u)
							}
						}
						gt.insertBefore(e, gt.firstChild),
							gt.appendChild(i),
							Ct = gt.children
					}
				}(),
				function () {
					if (!rt)
						for (var e = he, n = he + Math.min(wt, zt); e < n; e++) {
							var i = Ct[e];
							i.style.left = 100 * (e - he) / zt + "%",
								m(i, st),
								y(i, dt)
						}
					if (ht && (G || Ot ? (f(ne, "#" + Ne + " > .tns-item", "font-size:" + l.getComputedStyle(Ct[0]).fontSize + ";", d(ne)),
						f(ne, "#" + Ne, "font-size:0;", d(ne))) : rt && v(Ct, (function (t, e) {
							var n;
							t.style.marginLeft = (n = e,
								V ? V + "(" + 100 * n + "% / " + ue + ")" : 100 * n / ue + "%")
						}
						))),
						Q) {
						if (K) {
							var a = vt && t.autoHeight ? Yn(t.speed) : "";
							f(ne, "#" + Ne + "-mw", a, d(ne))
						}
						a = jn(t.edgePadding, t.gutter, t.fixedWidth, t.speed, t.autoHeight),
							f(ne, "#" + Ne + "-iw", a, d(ne)),
							rt && (a = ht && !Ot ? "width:" + Vn(t.fixedWidth, t.gutter, t.items) + ";" : "",
								K && (a += Yn(jt)),
								f(ne, "#" + Ne, a, d(ne))),
							a = ht && !Ot ? Gn(t.fixedWidth, t.gutter, t.items) : "",
							t.gutter && (a += Qn(t.gutter)),
							rt || (K && (a += Yn(jt)),
								U && (a += Kn(jt))),
							a && f(ne, "#" + Ne + " > .tns-item", a, d(ne))
					} else {
						rt && Qt && (vt.style[K] = jt / 1e3 + "s"),
							yt.style.cssText = jn(kt, Rt, Dt, Qt),
							rt && ht && !Ot && (gt.style.width = Vn(Dt, Rt, zt));
						a = ht && !Ot ? Gn(Dt, Rt, zt) : "";
						Rt && (a += Qn(Rt)),
							a && f(ne, "#" + Ne + " > .tns-item", a, d(ne))
					}
					if (it && Q)
						for (var r in it) {
							r = parseInt(r);
							var o = it[r]
								, u = (a = "",
									"")
								, s = ""
								, c = ""
								, p = ""
								, h = Ot ? null : Fn("items", r)
								, g = Fn("fixedWidth", r)
								, b = Fn("speed", r)
								, x = Fn("edgePadding", r)
								, C = Fn("autoHeight", r)
								, w = Fn("gutter", r);
							K && vt && Fn("autoHeight", r) && "speed" in o && (u = "#" + Ne + "-mw{" + Yn(b) + "}"),
								("edgePadding" in o || "gutter" in o) && (s = "#" + Ne + "-iw{" + jn(x, w, g, b, C) + "}"),
								rt && ht && !Ot && ("fixedWidth" in o || "items" in o || Dt && "gutter" in o) && (c = "width:" + Vn(g, w, h) + ";"),
								K && "speed" in o && (c += Yn(b)),
								c && (c = "#" + Ne + "{" + c + "}"),
								("fixedWidth" in o || Dt && "gutter" in o || !rt && "items" in o) && (p += Gn(g, w, h)),
								"gutter" in o && (p += Qn(w)),
								!rt && "speed" in o && (K && (p += Yn(b)),
									U && (p += Kn(b))),
								p && (p = "#" + Ne + " > .tns-item{" + p + "}"),
								(a = u + s + c + p) && ne.insertRule("@media (min-width: " + r / 16 + "em) {" + a + "}", ne.cssRules.length)
						}
				}(),
				Jn();
			var Sn = Gt ? rt ? function () {
				var t = ye
					, e = ge;
				t += Wt,
					e -= Wt,
					kt ? (t += 1,
						e -= 1) : Dt && (It + Rt) % (Dt + Rt) && (e -= 1),
					oe && (e < he ? he -= wt : he < t && (he += wt))
			}
				: function () {
					if (ge < he)
						for (; ye + wt <= he;)
							he -= wt;
					else if (he < ye)
						for (; he <= ge - wt;)
							he += wt
				}
				: function () {
					he = Math.max(ye, Math.min(ge, he))
				}
				, Hn = rt ? function () {
					var t, e, n, i, a, r, o, u, l, s, c;
					Ai(gt, ""),
						K || !jt ? (Oi(),
							jt && N(gt) || Ii()) : (t = gt,
								e = fe,
								n = de,
								i = ve,
								a = Si(),
								r = jt,
								o = Ii,
								u = Math.min(r, 10),
								l = 0 <= a.indexOf("%") ? "%" : "px",
								a = a.replace(l, ""),
								s = Number(t.style[e].replace(n, "").replace(i, "").replace(l, "")),
								c = (a - s) / r * u,
								setTimeout((function a() {
									r -= u,
										s += c,
										t.style[e] = n + s + l + i,
										0 < r ? setTimeout(a, u) : o()
								}
								), u)),
						ht || ta()
				}
					: function () {
						re = [];
						var t = {};
						t[Z] = t[$] = Ii,
							D(Ct[me], t),
							O(Ct[he], t),
							Di(me, st, ct, !0),
							Di(he, dt, st),
							Z && $ && jt && N(gt) || Ii()
					}
				;
			return {
				version: "2.9.2",
				getInfo: ia,
				events: Te,
				goTo: Pi,
				play: function () {
					_t && !mn && (ji(),
						gn = !1)
				},
				pause: function () {
					mn && (Vi(),
						gn = !0)
				},
				isOn: Tt,
				updateSliderHeight: bi,
				refresh: Jn,
				destroy: function () {
					if (ne.disabled = !0,
						ne.ownerNode && ne.ownerNode.remove(),
						D(l, {
							resize: ti
						}),
						Ft && D(n, Ie),
						_e && D(_e, Oe),
						rn && D(rn, De),
						D(gt, ke),
						D(gt, Re),
						Cn && D(Cn, {
							click: Gi
						}),
						_t && clearInterval(hn),
						rt && Z) {
						var e = {};
						e[Z] = Ii,
							D(gt, e)
					}
					Jt && D(gt, Pe),
						Ut && D(gt, ze);
					var i = [xt, Ze, en, nn, on, wn];
					for (var a in et.forEach((function (e, n) {
						var a = "container" === e ? mt : t[e];
						if ("object" == typeof a && a) {
							var r = !!a.previousElementSibling && a.previousElementSibling
								, o = a.parentNode;
							a.outerHTML = i[n],
								t[e] = r ? r.nextElementSibling : o.firstElementChild
						}
					}
					)),
						et = st = ct = ft = dt = ht = mt = yt = gt = bt = xt = Ct = wt = pt = Mt = Ot = Dt = kt = Rt = It = zt = Wt = qt = Ft = jt = Vt = Gt = Qt = ne = ie = Et = re = oe = ue = le = se = ce = fe = de = ve = pe = he = me = ye = ge = xe = Ce = we = Me = Te = Ee = Ne = Ae = Le = Be = Se = He = Oe = De = ke = Re = Ie = Pe = ze = We = qe = Fe = je = Ve = Ge = Qe = Xe = Ye = Nt = Xt = Yt = _e = Ze = $e = tn = Je = Ue = Kt = rn = on = an = un = ln = sn = cn = fn = dn = vn = pn = _t = Zt = xn = $t = te = Cn = wn = ee = Mn = hn = mn = yn = gn = bn = Nn = An = Tn = Ln = En = Bn = Jt = Ut = null,
						this)
						"rebuild" !== a && (this[a] = null);
					Tt = !1
				},
				rebuild: function () {
					return R(a(t, nt))
				}
			}
		}
		function On(t) {
			t && (Xt = Kt = Jt = Ut = Ft = _t = te = ee = !1)
		}
		function Dn() {
			for (var t = rt ? he - oe : he; t < 0;)
				t += wt;
			return t % wt + 1
		}
		function kn(t) {
			return t = t ? Math.max(0, Math.min(Gt ? wt - 1 : wt - zt, t)) : 0,
				rt ? t + oe : t
		}
		function Rn(t) {
			for (null == t && (t = he),
				rt && (t -= oe); t < 0;)
				t += wt;
			return Math.floor(t % wt)
		}
		function In() {
			var t, e = Rn();
			return t = Fe ? e : Dt || Ot ? Math.ceil((e + 1) * un / wt - 1) : Math.floor(e / zt),
				!Gt && rt && he === ge && (t = un - 1),
				t
		}
		function Pn() {
			return l.innerWidth || n.documentElement.clientWidth || n.body.clientWidth
		}
		function zn(t) {
			return "top" === t ? "afterbegin" : "beforeend"
		}
		function Wn() {
			var t = kt ? 2 * kt - Rt : 0;
			return function t(e) {
				if (null != e) {
					var i, a, r = n.createElement("div");
					return e.appendChild(r),
						a = (i = r.getBoundingClientRect()).right - i.left,
						r.remove(),
						a || t(e.parentNode)
				}
			}(bt) - t
		}
		function qn(e) {
			if (t[e])
				return !0;
			if (it)
				for (var n in it)
					if (it[n][e])
						return !0;
			return !1
		}
		function Fn(e, n) {
			if (null == n && (n = Mt),
				"items" === e && Dt)
				return Math.floor((It + Rt) / (Dt + Rt)) || 1;
			var i = t[e];
			if (it)
				for (var a in it)
					n >= parseInt(a) && e in it[a] && (i = it[a][e]);
			return "slideBy" === e && "page" === i && (i = Fn("items")),
				rt || "slideBy" !== e && "items" !== e || (i = Math.floor(i)),
				i
		}
		function jn(t, e, n, i, a) {
			var r = "";
			if (void 0 !== t) {
				var o = t;
				e && (o -= e),
					r = ht ? "margin: 0 " + o + "px 0 " + t + "px;" : "margin: " + t + "px 0 " + o + "px 0;"
			} else if (e && !n) {
				var u = "-" + e + "px";
				r = "margin: 0 " + (ht ? u + " 0 0" : "0 " + u + " 0") + ";"
			}
			return !rt && a && K && i && (r += Yn(i)),
				r
		}
		function Vn(t, e, n) {
			return t ? (t + e) * ue + "px" : V ? V + "(" + 100 * ue + "% / " + n + ")" : 100 * ue / n + "%"
		}
		function Gn(t, e, n) {
			var i;
			if (t)
				i = t + e + "px";
			else {
				rt || (n = Math.floor(n));
				var a = rt ? ue : n;
				i = V ? V + "(100% / " + a + ")" : 100 / a + "%"
			}
			return i = "width:" + i,
				"inner" !== at ? i + ";" : i + " !important;"
		}
		function Qn(t) {
			var e = "";
			return !1 !== t && (e = (ht ? "padding-" : "margin-") + (ht ? "right" : "bottom") + ": " + t + "px;"),
				e
		}
		function Xn(t, e) {
			var n = t.substring(0, t.length - e).toLowerCase();
			return n && (n = "-" + n + "-"),
				n
		}
		function Yn(t) {
			return Xn(K, 18) + "transition-duration:" + t / 1e3 + "s;"
		}
		function Kn(t) {
			return Xn(U, 17) + "animation-duration:" + t / 1e3 + "s;"
		}
		function Jn() {
			if (qn("autoHeight") || Ot || !ht) {
				var t = gt.querySelectorAll("img");
				v(t, (function (t) {
					var e = t.src;
					ie || (e && e.indexOf("data:image") < 0 ? (t.src = "",
						O(t, Ye),
						m(t, "loading"),
						t.src = e) : di(t))
				}
				)),
					e((function () {
						mi(M(t), (function () {
							Nt = !0
						}
						))
					}
					)),
					qn("autoHeight") && (t = pi(he, Math.min(he + zt - 1, ue - 1))),
					ie ? Un() : e((function () {
						mi(M(t), Un)
					}
					))
			} else
				rt && Hi(),
					Zn(),
					$n()
		}
		function Un() {
			if (Ot) {
				var t = Gt ? he : wt - 1;
				!function e() {
					var n = Ct[t].getBoundingClientRect().left
						, i = Ct[t - 1].getBoundingClientRect().right;
					Math.abs(n - i) <= 1 ? _n() : setTimeout((function () {
						e()
					}
					), 16)
				}()
			} else
				_n()
		}
		function _n() {
			ht && !Ot || (xi(),
				Ot ? (se = Bi(),
					Be && (Se = ni()),
					ge = pe(),
					On(Ae || Se)) : ta()),
				rt && Hi(),
				Zn(),
				$n()
		}
		function Zn() {
			if (Ci(),
				mt.insertAdjacentHTML("afterbegin", '<div class="tns-liveregion tns-visually-hidden" aria-live="polite" aria-atomic="true"><span class="tns-liveregion-inner"><span class="sr-only">slide</span> <span class="current">' + si() + "</span>/" + wt + "</span></div>"),
				At = mt.querySelector(".tns-liveregion .current"),
				je) {
				var e = _t ? "stop" : "start";
				Cn ? C(Cn, {
					"data-action": e
				}) : t.autoplayButtonOutput && (mt.insertAdjacentHTML(zn(t.autoplayPosition), '<button type="button" data-action="' + e + '">' + Mn[0] + e + Mn[1] + $t[0] + "</button>"),
					Cn = mt.querySelector("[data-action]")),
					Cn && O(Cn, {
						click: Gi
					}),
					_t && (ji(),
						te && O(gt, ke),
						ee && O(gt, Re))
			}
			if (qe) {
				if (rn)
					C(rn, {
						"aria-label": "Carousel Pagination"
					}),
						v(an = rn.children, (function (t, e) {
							C(t, {
								"data-nav": e,
								tabindex: "-1",
								"aria-label": vn + (e + 1),
								"aria-controls": Ne
							})
						}
						));
				else {
					for (var n = "", i = Fe ? "" : 'style="display:none"', a = 0; a < wt; a++)
						n += '<button type="button" data-nav="' + a + '" tabindex="-1" aria-controls="' + Ne + '" ' + i + ' aria-label="' + vn + (a + 1) + '"></button>';
					n = '<div class="tns-nav" aria-label="Carousel Pagination">' + n + "</div>",
						mt.insertAdjacentHTML(zn(t.navPosition), n),
						rn = mt.querySelector(".tns-nav"),
						an = rn.children
				}
				if (na(),
					K) {
					var r = K.substring(0, K.length - 18).toLowerCase()
						, o = "transition: all " + jt / 1e3 + "s";
					r && (o = "-" + r + "-" + o),
						f(ne, "[aria-controls^=" + Ne + "-item]", o, d(ne))
				}
				C(an[cn], {
					"aria-label": vn + (cn + 1) + pn
				}),
					w(an[cn], "tabindex"),
					m(an[cn], dn),
					O(rn, De)
			}
			We && (_e || $e && tn || (mt.insertAdjacentHTML(zn(t.controlsPosition), '<div class="tns-controls" aria-label="Carousel Navigation" tabindex="0"><button type="button" class="btn btn-controls btn--ghost btn--primary btn--icon-only" data-controls="prev" tabindex="-1" aria-controls="' + Ne + '"><span class="btn-text sr-only">' + Yt[0] + '</span></button><button type="button" class="btn btn-controls btn--ghost btn--primary btn--icon-only" data-controls="next" tabindex="-1" aria-controls="' + Ne + '"><span class="btn-text sr-only">' + Yt[1] + "</span></button></div>"),
				_e = mt.querySelector(".tns-controls")),
				$e && tn || ($e = _e.children[0],
					tn = _e.children[1]),
				t.controlsContainer && C(_e, {
					"aria-label": "Carousel Navigation",
					tabindex: "0"
				}),
				(t.controlsContainer || t.prevButton && t.nextButton) && C([$e, tn], {
					"aria-controls": Ne,
					tabindex: "-1"
				}),
				(t.controlsContainer || t.prevButton && t.nextButton) && (C($e, {
					"data-controls": "prev"
				}),
					C(tn, {
						"data-controls": "next"
					})),
				Je = Mi($e),
				Ue = Mi(tn),
				Ni(),
				_e ? O(_e, Oe) : (O($e, Oe),
					O(tn, Oe))),
				ai()
		}
		function $n() {
			if (rt && Z) {
				var e = {};
				e[Z] = Ii,
					O(gt, e)
			}
			Jt && O(gt, Pe, t.preventScrollOnTouch),
				Ut && O(gt, ze),
				Ft && O(n, Ie),
				"inner" === at ? Te.on("outerResized", (function () {
					ei(),
						Te.emit("innerLoaded", ia())
				}
				)) : (it || Dt || Ot || Qt || !ht) && O(l, {
					resize: ti
				}),
				Qt && ("outer" === at ? Te.on("innerLoaded", hi) : Ae || hi()),
				fi(),
				Ae ? ui() : Se && oi(),
				Te.on("indexChanged", yi),
				"inner" === at && Te.emit("innerLoaded", ia()),
				"function" == typeof Me && Me(ia()),
				Tt = !0
		}
		function ti(t) {
			e((function () {
				ei(Xi(t))
			}
			))
		}
		function ei(e) {
			if (Tt) {
				"outer" === at && Te.emit("outerResized", ia(e)),
					Mt = Pn();
				var i, a = pt, r = !1;
				it && (ii(),
					(i = a !== pt) && Te.emit("newBreakpointStart", ia(e)));
				var o, u, l, s, c = zt, p = Ae, h = Se, g = Ft, b = Xt, x = Kt, C = Jt, w = Ut, M = _t, N = te, A = ee, L = he;
				if (i) {
					var B = Dt
						, S = Qt
						, H = Yt
						, k = Pt
						, R = $t;
					if (!Q)
						var I = Rt
							, P = kt
				}
				if (Ft = Fn("arrowKeys"),
					Xt = Fn("controls"),
					Kt = Fn("nav"),
					Jt = Fn("touch"),
					Pt = Fn("center"),
					Ut = Fn("mouseDrag"),
					_t = Fn("autoplay"),
					te = Fn("autoplayHoverPause"),
					ee = Fn("autoplayResetOnVisibility"),
					i && (Ae = Fn("disable"),
						Dt = Fn("fixedWidth"),
						jt = Fn("speed"),
						Qt = Fn("autoHeight"),
						Yt = Fn("controlsText"),
						$t = Fn("autoplayText"),
						Zt = Fn("autoplayTimeout"),
						Q || (kt = Fn("edgePadding"),
							Rt = Fn("gutter"))),
					On(Ae),
					It = Wn(),
					ht && !Ot || Ae || (xi(),
						ht || (ta(),
							r = !0)),
					(Dt || Ot) && (se = Bi(),
						ge = pe()),
					(i || Dt) && (zt = Fn("items"),
						Wt = Fn("slideBy"),
						(u = zt !== c) && (Dt || Ot || (ge = pe()),
							Sn())),
					i && Ae !== p && (Ae ? ui() : function () {
						if (Le) {
							if (ne.disabled = !1,
								gt.className += Ee,
								Hi(),
								Gt)
								for (var t = oe; t--;)
									rt && E(Ct[t]),
										E(Ct[ue - t - 1]);
							if (!rt)
								for (var e = he, n = he + wt; e < n; e++) {
									var i = Ct[e]
										, a = e < he + zt ? st : dt;
									i.style.left = 100 * (e - he) / zt + "%",
										m(i, a)
								}
							ri(),
								Le = !1
						}
					}()),
					Be && (i || Dt || Ot) && (Se = ni()) !== h && (Se ? (Oi(Si(kn(0))),
						oi()) : (function () {
							if (He) {
								if (kt && Q && (yt.style.margin = ""),
									oe)
									for (var t = "tns-transparent", e = oe; e--;)
										rt && y(Ct[e], t),
											y(Ct[ue - e - 1], t);
								ri(),
									He = !1
							}
						}(),
							r = !0)),
					On(Ae || Se),
					_t || (te = ee = !1),
					Ft !== g && (Ft ? O(n, Ie) : D(n, Ie)),
					Xt !== b && (Xt ? _e ? E(_e) : ($e && E($e),
						tn && E(tn)) : _e ? T(_e) : ($e && T($e),
							tn && T(tn))),
					Kt !== x && (Kt ? E(rn) : T(rn)),
					Jt !== C && (Jt ? O(gt, Pe, t.preventScrollOnTouch) : D(gt, Pe)),
					Ut !== w && (Ut ? O(gt, ze) : D(gt, ze)),
					_t !== M && (_t ? (Cn && E(Cn),
						mn || gn || ji()) : (Cn && T(Cn),
							mn && Vi())),
					te !== N && (te ? O(gt, ke) : D(gt, ke)),
					ee !== A && (ee ? O(n, Re) : D(n, Re)),
					i) {
					if (Dt === B && Pt === k || (r = !0),
						Qt !== S && (Qt || (yt.style.height = "")),
						Xt && Yt !== H && ($e.innerHTML = Yt[0],
							tn.innerHTML = Yt[1]),
						Cn && $t !== R) {
						var z = _t ? 1 : 0
							, W = Cn.innerHTML
							, q = W.length - R[z].length;
						W.substring(q) === R[z] && (Cn.innerHTML = W.substring(0, q) + $t[z])
					}
				} else
					Pt && (Dt || Ot) && (r = !0);
				if ((u || Dt && !Ot) && (un = ea(),
					na()),
					(o = he !== L) ? (Te.emit("indexChanged", ia()),
						r = !0) : u ? o || yi() : (Dt || Ot) && (fi(),
							Ci(),
							li()),
					u && !rt && function () {
						for (var t = he + Math.min(wt, zt), e = ue; e--;) {
							var n = Ct[e];
							he <= e && e < t ? (m(n, "tns-moving"),
								n.style.left = 100 * (e - he) / zt + "%",
								m(n, st),
								y(n, dt)) : n.style.left && (n.style.left = "",
									m(n, dt),
									y(n, st)),
								y(n, ct)
						}
						setTimeout((function () {
							v(Ct, (function (t) {
								y(t, "tns-moving")
							}
							))
						}
						), 300)
					}(),
					!Ae && !Se) {
					if (i && !Q && (kt === P && Rt === I || (yt.style.cssText = jn(kt, Rt, Dt, jt, Qt)),
						ht)) {
						rt && (gt.style.width = Vn(Dt, Rt, zt));
						var F = Gn(Dt, Rt, zt) + Qn(Rt);
						s = d(l = ne) - 1,
							"deleteRule" in l ? l.deleteRule(s) : l.removeRule(s),
							f(ne, "#" + Ne + " > .tns-item", F, d(ne))
					}
					Qt && hi(),
						r && (Hi(),
							me = he)
				}
				i && Te.emit("newBreakpointEnd", ia(e))
			}
		}
		function ni() {
			if (!Dt && !Ot)
				return wt <= (Pt ? zt - (zt - 1) / 2 : zt);
			var t = Dt ? (Dt + Rt) * wt : Et[wt]
				, e = kt ? It + 2 * kt : It + Rt;
			return Pt && (e -= Dt ? (It - Dt) / 2 : (It - (Et[he + 1] - Et[he] - Rt)) / 2),
				t <= e
		}
		function ii() {
			for (var t in pt = 0,
				it)
				(t = parseInt(t)) <= Mt && (pt = t)
		}
		function ai() {
			!_t && Cn && T(Cn),
				!Kt && rn && T(rn),
				Xt || (_e ? T(_e) : ($e && T($e),
					tn && T(tn)))
		}
		function ri() {
			_t && Cn && E(Cn),
				Kt && rn && E(rn),
				Xt && (_e ? E(_e) : ($e && E($e),
					tn && E(tn)))
		}
		function oi() {
			if (!He) {
				if (kt && (yt.style.margin = "0px"),
					oe)
					for (var t = "tns-transparent", e = oe; e--;)
						rt && m(Ct[e], t),
							m(Ct[ue - e - 1], t);
				ai(),
					He = !0
			}
		}
		function ui() {
			if (!Le) {
				if (ne.disabled = !0,
					gt.className = gt.className.replace(Ee.substring(1), ""),
					w(gt, ["style"]),
					Gt)
					for (var t = oe; t--;)
						rt && T(Ct[t]),
							T(Ct[ue - t - 1]);
				if (ht && rt || w(yt, ["style"]),
					!rt)
					for (var e = he, n = he + wt; e < n; e++) {
						var i = Ct[e];
						w(i, ["style"]),
							y(i, st),
							y(i, dt)
					}
				ai(),
					Le = !0
			}
		}
		function li() {
			var t = si();
			At.innerHTML !== t && (At.innerHTML = t)
		}
		function si() {
			var t = ci()
				, e = t[0] + 1
				, n = t[1] + 1;
			return e === n ? e + "" : e + " to " + n
		}
		function ci(t) {
			null == t && (t = Si());
			var e, n, i, a = he;
			if (Pt || kt ? (Ot || Dt) && (n = -(parseFloat(t) + kt),
				i = n + It + 2 * kt) : Ot && (n = Et[he],
					i = n + It),
				Ot)
				Et.forEach((function (t, r) {
					r < ue && ((Pt || kt) && t <= n + .5 && (a = r),
						.5 <= i - t && (e = r))
				}
				));
			else {
				if (Dt) {
					var r = Dt + Rt;
					Pt || kt ? (a = Math.floor(n / r),
						e = Math.ceil(i / r - 1)) : e = a + Math.ceil(It / r) - 1
				} else if (Pt || kt) {
					var o = zt - 1;
					if (Pt ? (a -= o / 2,
						e = he + o / 2) : e = he + o,
						kt) {
						var u = kt * zt / It;
						a -= u,
							e += u
					}
					a = Math.floor(a),
						e = Math.ceil(e)
				} else
					e = a + zt - 1;
				a = Math.max(a, 0),
					e = Math.min(e, ue - 1)
			}
			return [a, e]
		}
		function fi() {
			if (ie && !Ae) {
				var t = ci();
				t.push(ae),
					pi.apply(null, t).forEach((function (t) {
						if (!h(t, Xe)) {
							var e = {};
							e[Z] = function (t) {
								t.stopPropagation()
							}
								,
								O(t, e),
								O(t, Ye),
								t.src = b(t, "data-src");
							var n = b(t, "data-srcset");
							n && (t.srcset = n),
								m(t, "loading")
						}
					}
					))
			}
		}
		function di(t) {
			m(t, "loaded"),
				vi(t)
		}
		function vi(t) {
			m(t, Xe),
				y(t, "loading"),
				D(t, Ye)
		}
		function pi(t, e, n) {
			var i = [];
			for (n || (n = "img"); t <= e;)
				v(Ct[t].querySelectorAll(n), (function (t) {
					i.push(t)
				}
				)),
					t++;
			return i
		}
		function hi() {
			var t = pi.apply(null, ci());
			e((function () {
				mi(t, bi)
			}
			))
		}
		function mi(t, n) {
			return Nt ? n() : (t.forEach((function (e, n) {
				!ie && e.complete && vi(e),
					h(e, Xe) && t.splice(n, 1)
			}
			)),
				t.length ? void e((function () {
					mi(t, n)
				}
				)) : n())
		}
		function yi() {
			fi(),
				Ci(),
				li(),
				Ni(),
				function () {
					if (Kt && (cn = 0 <= sn ? sn : In(),
						sn = -1,
						cn !== fn)) {
						var t = an[fn]
							, e = an[cn];
						C(t, {
							tabindex: "-1",
							"aria-label": vn + (fn + 1)
						}),
							y(t, dn),
							C(e, {
								"aria-label": vn + (cn + 1) + pn
							}),
							w(e, "tabindex"),
							m(e, dn),
							fn = cn
					}
				}()
		}
		function gi(t, e) {
			for (var n = [], i = t, a = Math.min(t + e, ue); i < a; i++)
				n.push(Ct[i].offsetHeight);
			return Math.max.apply(null, n)
		}
		function bi() {
			var t = Qt ? gi(he, zt) : gi(oe, wt)
				, e = vt || yt;
			e.style.height !== t && (e.style.height = t + "px")
		}
		function xi() {
			Et = [0];
			var t = ht ? "left" : "top"
				, e = ht ? "right" : "bottom"
				, n = Ct[0].getBoundingClientRect()[t];
			v(Ct, (function (i, a) {
				a && Et.push(i.getBoundingClientRect()[t] - n),
					a === ue - 1 && Et.push(i.getBoundingClientRect()[e] - n)
			}
			))
		}
		function Ci() {
			var t = ci()
				, e = t[0]
				, n = t[1];
			v(Ct, (function (t, i) {
				e <= i && i <= n ? g(t, "aria-hidden") && (w(t, ["aria-hidden", "tabindex"]),
					m(t, Qe)) : g(t, "aria-hidden") || (C(t, {
						"aria-hidden": "true",
						tabindex: "-1"
					}),
						y(t, Qe))
			}
			))
		}
		function wi(t) {
			return t.nodeName.toLowerCase()
		}
		function Mi(t) {
			return "button" === wi(t)
		}
		function Ti(t) {
			return "true" === t.getAttribute("aria-disabled")
		}
		function Ei(t, e, n) {
			t ? e.disabled = n : e.setAttribute("aria-disabled", n.toString())
		}
		function Ni() {
			if (Xt && !Vt && !Gt) {
				var t = Je ? $e.disabled : Ti($e)
					, e = Ue ? tn.disabled : Ti(tn)
					, n = he <= ye
					, i = !Vt && ge <= he;
				n && !t && Ei(Je, $e, !0),
					!n && t && Ei(Je, $e, !1),
					i && !e && Ei(Ue, tn, !0),
					!i && e && Ei(Ue, tn, !1)
			}
		}
		function Ai(t, e) {
			K && (t.style[K] = e)
		}
		function Li(t) {
			return null == t && (t = he),
				Ot ? (It - (kt ? Rt : 0) - (Et[t + 1] - Et[t] - Rt)) / 2 : Dt ? (It - Dt) / 2 : (zt - 1) / 2
		}
		function Bi() {
			var t = It + (kt ? Rt : 0) - (Dt ? (Dt + Rt) * ue : Et[ue]);
			return Pt && !Gt && (t = Dt ? -(Dt + Rt) * (ue - 1) - Li() : Li(ue - 1) - Et[ue - 1]),
				0 < t && (t = 0),
				t
		}
		function Si(t) {
			var e;
			if (null == t && (t = he),
				ht && !Ot)
				if (Dt)
					e = -(Dt + Rt) * t,
						Pt && (e += Li());
				else {
					var n = X ? ue : zt;
					Pt && (t -= Li()),
						e = 100 * -t / n
				}
			else
				e = -Et[t],
					Pt && Ot && (e += Li());
			return le && (e = Math.max(e, se)),
				e + (!ht || Ot || Dt ? "px" : "%")
		}
		function Hi(t) {
			Ai(gt, "0s"),
				Oi(t)
		}
		function Oi(t) {
			null == t && (t = Si()),
				gt.style[fe] = de + t + ve
		}
		function Di(t, e, n, i) {
			var a = t + zt;
			Gt || (a = Math.min(a, ue));
			for (var r = t; r < a; r++) {
				var o = Ct[r];
				i || (o.style.left = 100 * (r - he) / zt + "%"),
					ft && J && (o.style[J] = o.style[_] = ft * (r - t) / 1e3 + "s"),
					y(o, e),
					m(o, n),
					i && re.push(o)
			}
		}
		function ki(t, e) {
			ce && Sn(),
				(he !== me || e) && (Te.emit("indexChanged", ia()),
					Te.emit("transitionStart", ia()),
					Qt && hi(),
					mn && t && 0 <= ["click", "keydown"].indexOf(t.type) && Vi(),
					we = !0,
					Hn())
		}
		function Ri(t) {
			return t.toLowerCase().replace(/-/g, "")
		}
		function Ii(t) {
			if (rt || we) {
				if (Te.emit("transitionEnd", ia(t)),
					!rt && 0 < re.length)
					for (var e = 0; e < re.length; e++) {
						var n = re[e];
						n.style.left = "",
							_ && J && (n.style[_] = "",
								n.style[J] = ""),
							y(n, ct),
							m(n, dt)
					}
				if (!t || !rt && t.target.parentNode === gt || t.target === gt && Ri(t.propertyName) === Ri(fe)) {
					if (!ce) {
						var i = he;
						Sn(),
							he !== i && (Te.emit("indexChanged", ia()),
								Hi())
					}
					"inner" === at && Te.emit("innerLoaded", ia()),
						we = !1,
						me = he
				}
			}
		}
		function Pi(t, e) {
			if (!Se)
				if ("prev" === t)
					zi(e, -1);
				else if ("next" === t)
					zi(e, 1);
				else {
					if (we) {
						if (be)
							return;
						Ii()
					}
					var n = Rn()
						, i = 0;
					if ("first" === t ? i = -n : "last" === t ? i = rt ? wt - zt - n : wt - 1 - n : ("number" != typeof t && (t = parseInt(t)),
						isNaN(t) || (e || (t = Math.max(0, Math.min(wt - 1, t))),
							i = t - n)),
						!rt && i && Math.abs(i) < zt) {
						var a = 0 < i ? 1 : -1;
						i += ye <= he + i - wt ? wt * a : 2 * wt * a * -1
					}
					he += i,
						rt && Gt && (he < ye && (he += wt),
							ge < he && (he -= wt)),
						Rn(he) !== Rn(me) && ki(e)
				}
		}
		function zi(t, e) {
			if (we) {
				if (be)
					return;
				Ii()
			}
			var n;
			if (!e) {
				for (var i = Yi(t = Xi(t)); i !== _e && [$e, tn].indexOf(i) < 0;)
					i = i.parentNode;
				var a = [$e, tn].indexOf(i);
				0 <= a && (n = !0,
					e = 0 === a ? -1 : 1)
			}
			if (Vt) {
				if (he === ye && -1 === e)
					return void Pi("last", t);
				if (he === ge && 1 === e)
					return void Pi("first", t)
			}
			e && (he += Wt * e,
				Ot && (he = Math.floor(he)),
				ki(n || t && "keydown" === t.type ? t : null))
		}
		function Wi() {
			hn = setInterval((function () {
				zi(null, xn)
			}
			), Zt),
				mn = !0
		}
		function qi() {
			clearInterval(hn),
				mn = !1
		}
		function Fi(t, e) {
			C(Cn, {
				"data-action": t
			}),
				Cn.innerHTML = Mn[0] + t + Mn[1] + e
		}
		function ji() {
			Wi(),
				Cn && Fi("stop", $t[1])
		}
		function Vi() {
			qi(),
				Cn && Fi("start", $t[0])
		}
		function Gi() {
			mn ? (Vi(),
				gn = !0) : (ji(),
					gn = !1)
		}
		function Qi(t) {
			t.focus()
		}
		function Xi(t) {
			return Ki(t = t || l.event) ? t.changedTouches[0] : t
		}
		function Yi(t) {
			return t.target || l.event.srcElement
		}
		function Ki(t) {
			return 0 <= t.type.indexOf("touch")
		}
		function Ji(t) {
			t.preventDefault ? t.preventDefault() : t.returnValue = !1
		}
		function Ui() {
			return r = An.y - Nn.y,
				o = An.x - Nn.x,
				e = Math.atan2(r, o) * (180 / Math.PI),
				i = !1,
				90 - (n = xe) <= (a = Math.abs(90 - Math.abs(e))) ? i = "horizontal" : a <= n && (i = "vertical"),
				i === t.axis;
			var e, n, i, a, r, o
		}
		function _i(t) {
			if (we) {
				if (be)
					return;
				Ii()
			}
			_t && mn && qi(),
				Ln = !0,
				En && (i(En),
					En = null);
			var e = Xi(t);
			Te.emit(Ki(t) ? "touchStart" : "dragStart", ia(t)),
				!Ki(t) && 0 <= ["img", "a"].indexOf(wi(Yi(t))) && Ji(t),
				An.x = Nn.x = e.clientX,
				An.y = Nn.y = e.clientY,
				rt && (Tn = parseFloat(gt.style[fe].replace(de, "")),
					Ai(gt, "0s"))
		}
		function Zi(t) {
			if (Ln) {
				var n = Xi(t);
				An.x = n.clientX,
					An.y = n.clientY,
					rt ? En || (En = e((function () {
						!function t(n) {
							if (Ce) {
								if (i(En),
									Ln && (En = e((function () {
										t(n)
									}
									))),
									"?" === Ce && (Ce = Ui()),
									Ce) {
									!Ke && Ki(n) && (Ke = !0);
									try {
										n.type && Te.emit(Ki(n) ? "touchMove" : "dragMove", ia(n))
									} catch (t) { }
									var a = Tn
										, r = Bn(An, Nn);
									if (!ht || Dt || Ot)
										a += r,
											a += "px";
									else
										a += X ? r * zt * 100 / ((It + Rt) * ue) : 100 * r / (It + Rt),
											a += "%";
									gt.style[fe] = de + a + ve
								}
							} else
								Ln = !1
						}(t)
					}
					))) : ("?" === Ce && (Ce = Ui()),
						Ce && (Ke = !0)),
					("boolean" != typeof t.cancelable || t.cancelable) && Ke && t.preventDefault()
			}
		}
		function $i(n) {
			if (Ln) {
				En && (i(En),
					En = null),
					rt && Ai(gt, ""),
					Ln = !1;
				var a = Xi(n);
				An.x = a.clientX,
					An.y = a.clientY;
				var r = Bn(An, Nn);
				if (Math.abs(r)) {
					if (!Ki(n)) {
						var o = Yi(n);
						O(o, {
							click: function t(e) {
								Ji(e),
									D(o, {
										click: t
									})
							}
						})
					}
					rt ? En = e((function () {
						if (ht && !Ot) {
							var t = -r * zt / (It + Rt);
							t = 0 < r ? Math.floor(t) : Math.ceil(t),
								he += t
						} else {
							var e = -(Tn + r);
							if (e <= 0)
								he = ye;
							else if (e >= Et[ue - 1])
								he = ge;
							else
								for (var i = 0; i < ue && e >= Et[i];)
									e > Et[he = i] && r < 0 && (he += 1),
										i++
						}
						ki(n, r),
							Te.emit(Ki(n) ? "touchEnd" : "dragEnd", ia(n))
					}
					)) : Ce && zi(n, 0 < r ? -1 : 1)
				}
			}
			"auto" === t.preventScrollOnTouch && (Ke = !1),
				xe && (Ce = "?"),
				_t && !mn && Wi()
		}
		function ta() {
			(vt || yt).style.height = Et[he + zt] - Et[he] + "px"
		}
		function ea() {
			var t = Dt ? (Dt + Rt) * wt / It : wt / zt;
			return Math.min(Math.ceil(t), wt)
		}
		function na() {
			if (Kt && !Fe && un !== ln) {
				var t = ln
					, e = un
					, n = E;
				for (un < ln && (t = un,
					e = ln,
					n = T); t < e;)
					n(an[t]),
						t++;
				ln = un
			}
		}
		function ia(t) {
			return {
				container: gt,
				slideItems: Ct,
				navContainer: rn,
				navItems: an,
				controlsContainer: _e,
				hasControls: We,
				prevButton: $e,
				nextButton: tn,
				items: zt,
				slideBy: Wt,
				cloneCount: oe,
				slideCount: wt,
				slideCountNew: ue,
				index: he,
				indexCached: me,
				displayIndex: Dn(),
				navCurrentIndex: cn,
				navCurrentIndexCached: fn,
				pages: un,
				pagesCached: ln,
				sheet: ne,
				isOn: Tt,
				event: t || {}
			}
		}
		tt && console.warn("No slides found in", t.container)
	};
	return R
}();
