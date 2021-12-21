var board = require('./board');
var drag = require('./drag');
var util = require('./util');

function setDropMode(data, piece) {
  data.dropmode.active = true;
  data.dropmode.piece = piece;
  drag.cancel(data);
  board.setSelected(data, null);
}

function cancelDropMode(data) {
  data.dropmode.active = false;
  data.dropmode.piece = undefined;
}

function drop(data, e) {
  if (!data.dropmode.active) return;
  board.unsetPremove(data);
  board.unsetPredrop(data);

  var piece = data.dropmode.piece;

  if (piece) {
    data.pieces['a0'] = piece;
    var position = util.eventPosition(e);
    var bounds = data.bounds();
    var dest = position && board.getKeyAtDomPos(data, position, bounds);
    if (dest) board.dropNewPiece(data, 'a0', dest);
  }
}

module.exports = {
  setDropMode: setDropMode,
  cancelDropMode: cancelDropMode,
  drop: drop,
};
