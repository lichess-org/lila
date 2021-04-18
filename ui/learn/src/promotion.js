var m = require("mithril");
var chessground = require("./og/main");
var ground = require("./ground");
const { canPiecePromote } = require("shogiops/util");
const { parseChessSquare } = require("shogiops/compat");
var opposite = chessground.util.opposite;
var key2pos = chessground.util.key2pos;

var promoting = false;

function start(orig, dest, callback) {
  var piece = ground.pieces()[dest];
  if (!piece) return false;
  if (canPiecePromote(piece, parseChessSquare(orig), parseChessSquare(dest))){
    promoting = {
      orig: orig,
      dest: dest,
      role: piece.role,
      callback: callback,
    };
    m.redraw();
    return true;
  }
  return false;
}

function finish(role) {
  let prom = true;
  if (["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(role))
    prom = false;
  if (prom && promoting) ground.promote(promoting.dest, role);
  if (promoting.callback)
    promoting.callback(promoting.orig, promoting.dest, prom);
  promoting = false;
}

function renderPromotion(ctrl, dest, pieces, color, orientation, explain) {
  if (!promoting) return;

  var left = (9 - key2pos(dest)[0]) * 11.11 + 0.29;
  if (orientation === "sente") left = (key2pos(dest)[0] - 1) * 11.11 - 0.29;

  var vertical = color === orientation ? "top" : "bottom";

  return m("div#promotion-choice." + vertical, [
    pieces.map(function (serverRole, i) {
      var top = (i + key2pos(dest)[1]) * 11.11 - 0.29;
      if (orientation === "sente")
        top = (i + 9 - key2pos(dest)[1]) * 11.11 + 0.29;
      return m(
        "square",
        {
          style: vertical + ": " + top + "%;left: " + left + "%",
          onclick: function (e) {
            e.stopPropagation();
            finish(serverRole);
          },
        },
        m("piece." + serverRole + "." + color)
      );
    }),
  ]);
}

function promotesTo(role) {
  switch (role) {
    case "silver":
      return "promotedsilver";
    case "knight":
      return "promotedknight";
    case "lance":
      return "promotedlance";
    case "bishop":
      return "horse";
    case "rook":
      return "dragon";
    default:
      return "tokin";
  }
}

module.exports = {
  start: start,

  view: function (ctrl, stage) {
    if (!promoting) return;

    var pieces = [promotesTo(promoting.role), promoting.role];

    return renderPromotion(
      ctrl,
      promoting.dest,
      pieces,
      opposite(ground.data().turnColor),
      ground.data().orientation,
      stage.blueprint.explainPromotion
    );
  },

  reset: function () {
    promoting = false;
  },
};
