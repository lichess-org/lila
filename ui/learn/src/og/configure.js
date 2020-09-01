var merge = require("merge");
var board = require("./board");
var fen = require("./fen");

module.exports = function (data, config) {
  if (!config) return;

  // don't merge destinations. Just override.
  if (config.movable && config.movable.dests) delete data.movable.dests;

  merge.recursive(data, config);

  // if a fen was provided, replace the pieces
  if (data.fen) {
    data.pieces = fen.read(data.fen);
    data.check = config.check;
    data.drawable.shapes = [];
    delete data.fen;
  }

  if (data.check === true) board.setCheck(data);

  // forget about the last dropped piece
  data.movable.dropped = [];

  // fix move/premove dests
  if (data.selected) board.setSelected(data, data.selected);

  // no need for such short animations
  if (!data.animation.duration || data.animation.duration < 40)
    data.animation.enabled = false;

  if (!data.movable.rookCastle) {
    var rank = data.movable.color === "white" ? 1 : 9;
    var kingStartPos = "e" + rank;
    if (data.movable.dests) {
      var dests = data.movable.dests[kingStartPos];
      if (!dests || data.pieces[kingStartPos].role !== "king") return;
      data.movable.dests[kingStartPos] = dests.filter(function (d) {
        if (d === "a" + rank && dests.indexOf("c" + rank) !== -1) return false;
        if (d === "h" + rank && dests.indexOf("g" + rank) !== -1) return false;
        return true;
      });
    }
  }
};
