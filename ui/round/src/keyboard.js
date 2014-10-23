var k = require('mousetrap');

function replayPly(ctrl) {
  return ctrl.replay.active ? ctrl.replay.ply : ctrl.data.game.moves.length;
}

module.exports = {
  init: function(ctrl) {
    k.bind(['left', 'h'], function() {
      ctrl.replay.jump(replayPly(ctrl) - 1);
      m.redraw();
    });
    k.bind(['right', 'l'], function() {
      ctrl.replay.jump(replayPly(ctrl) + 1);
      m.redraw();
    });
    k.bind(['up', 'j'], function() {
      ctrl.replay.jump(1);
      m.redraw();
    });
    k.bind(['down', 'k'], function() {
      ctrl.replay.jump(ctrl.data.game.moves.length);
      m.redraw();
    });
  }
};
