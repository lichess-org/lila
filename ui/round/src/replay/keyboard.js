var k = require('mousetrap');
var m = require('mithril');

function replayPly(ctrl) {
  return ctrl.replay.active ? ctrl.replay.ply : ctrl.data.game.moves.length;
}

function preventing(f) {
  return function(e) {
    if (e.preventDefault) {
      e.preventDefault();
    } else {
      // internet explorer
      e.returnValue = false;
    }
    f();
  };
}

function prev(ctrl) {
  ctrl.replay.jump(replayPly(ctrl) - 1);
}

function next(ctrl) {
  ctrl.replay.jump(replayPly(ctrl) + 1);
}

module.exports = {
  prev: prev,
  next: next,
  init: function(ctrl) {
    k.bind(['left', 'h'], preventing(function() {
      prev(ctrl);
      m.redraw();
    }));
    k.bind(['right', 'l'], preventing(function() {
      next(ctrl);
      m.redraw();
    }));
    k.bind(['up', 'j'], preventing(function() {
      ctrl.replay.jump(1);
      m.redraw();
    }));
    k.bind(['down', 'k'], preventing(function() {
      ctrl.replay.jump(ctrl.data.game.moves.length);
      m.redraw();
    }));
  }
};
