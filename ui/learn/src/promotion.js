var m = require("mithril");
var chessground = require("./og/main");
var ground = require("./ground");
var opposite = chessground.util.opposite;
var key2pos = chessground.util.key2pos;

var promoting = false;

function start(orig, dest, callback) {
  var piece = ground.pieces()[dest];
  if (
    piece &&
    ["pawn", "lance", "knight", "silver", "bishop", "rook"].includes(
      piece.role
    ) &&
    (((["7", "8", "9"].includes(dest[1]) ||
      ["7", "8", "9"].includes(orig[1])) &&
      piece.color == "white") ||
      ((["1", "2", "3"].includes(dest[1]) ||
        ["1", "2", "3"].includes(orig[1])) &&
        piece.color === "black"))
  ) {
    console.log("PROMTOED");
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
  if (promoting) ground.promote(promoting.dest, role);
  if (promoting.callback)
    promoting.callback(promoting.orig, promoting.dest, role);
  promoting = false;
}

function renderPromotion(ctrl, dest, pieces, color, orientation, explain) {
  if (!promoting) return;

  var left = (9 - key2pos(dest)[0]) * 11.11 - 0.29;
  if (orientation === "white") left = key2pos(dest)[0] * 11.11;

  var vertical = color === orientation ? "top" : "bottom";

  return m("div#promotion-choice." + vertical, [
    pieces.map(function (serverRole, i) {
      return m(
        "square",
        {
          style:
            vertical +
            ": " +
            (i + key2pos(dest[1])) * 11.11 +
            "%;left: " +
            left +
            "%",
          onclick: function (e) {
            e.stopPropagation();
            finish(serverRole);
          },
        },
        m("piece." + serverRole + "." + color)
      );
    }),
    explain ? renderExplanation(ctrl, pieces) : null,
  ]);
}

function renderExplanation(ctrl, pieces) {
  return m("div.explanation", [
    m("h2", ctrl.trans.noarg("promotion")),
    m("p", ctrl.trans.noarg("yourPieceReachedThePromotionZone")),
    m("p", ctrl.trans.noarg("itNowPromotesToAStrongerPiece")),
  ]);
}

function promotesTo(role) {
  switch (role) {
    case "silver":
      return "promotedSilver";
    case "knight":
      return "promotedKnight";
    case "lance":
      return "promotedLance";
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
