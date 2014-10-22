var k = require('mousetrap');

module.exports = {
  init: function(ctrl) {
    k.bind(['left', 'h'], function() {
      ctrl.replay.jump(ctrl.replay.ply - 1);
      m.redraw();
    });
    k.bind(['right', 'l'], function() {
      ctrl.replay.jump(ctrl.replay.ply + 1);
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
