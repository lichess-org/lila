var m = require("mithril");

module.exports = {
  toLevel: function (l, it) {
    l.id = it + 1;
    if (!l.color) l.color = / b /.test(l.fen) ? "sente" : "gote";
    if (l.apples) l.detectCapture = false;
    else l.apples = [];
    if (typeof l.detectCapture === "undefined") l.detectCapture = "unprotected";
    return l;
  },
  assetUrl: $("body").data("asset-url") + "/assets/",
  arrow: function (vector, brush) {
    return {
      brush: brush || "paleGreen",
      orig: vector.slice(0, 2),
      dest: vector.slice(2, 4),
    };
  },
  circle: function (key, brush) {
    return {
      brush: brush || "green",
      orig: key,
    };
  },
  readKeys: function (keys) {
    return typeof keys === "string" ? keys.split(" ") : keys;
  },
  setFenTurn: function (fen, turn) {
    return fen.replace(/ (b|w) /, " " + turn + " ");
  },
  pieceImg: function (role) {
    return m("div.is2d.no-square", m("piece.sente." + role));
  },
  roundSvg: function (url) {
    return m(
      "div.round-svg",
      m("img", {
        src: url,
      })
    );
  },
  withLinebreaks: function (text) {
    return m.trust(lishogi.escapeHtml(text).replace(/\n/g, "<br>"));
  },
  decomposeUci: function (uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  },
};
