var util = require('chessground').util;

function capture(ctrl, key) {
  var ps = [];
  var pos = util.key2pos(key);
  for (var x = -1; x < 2; x++) {
    for (var y = -1; y < 2; y++) {
      var p = util.pos2key([pos[0] + x, pos[1] + y]);
      if (p) ps.push(p);
    }
  }
  ctrl.chessground.explode(ps);
};

module.exports = {
  capture: capture
};
