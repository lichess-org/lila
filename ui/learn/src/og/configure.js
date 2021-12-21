var merge = require('merge');
var board = require('./board');
var fen = require('./fen');

module.exports = function (data, config) {
  if (!config) return;

  // don't merge destinations. Just override.
  if (config.movable && config.movable.dests) delete data.movable.dests;

  // don't merge dropmode. Just override.
  if (config.dropmode) delete data.dropmode;

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

  // forget about selected pocket piece
  data.dropmode.active = false;

  // fix move/premove dests
  if (data.selected) board.setSelected(data, data.selected);

  // no need for such short animations
  if (!data.animation.duration || data.animation.duration < 40) data.animation.enabled = false;
};
