var winningChances = require('ceval').winningChances;
var m = require('mithril');

module.exports = function(root) {

  if (!root.ceval.enabled()) root.toggleCeval();

  var lastPathPlayed = m.prop();

  var onJump = function() {
    tryToPlay();
  };

  var checkCeval = function() {
    tryToPlay();
  };

  var tryToPlay = function() {
    if (turnColor() !== root.bottomColor() &&
      root.vm.node.ceval && root.vm.node.ceval.depth >= 16) {
      root.playUci(root.vm.node.ceval.best);
    }
  };

  var turnColor = function() {
    return root.chessground.data.movable.color;
  };

  return {
    onCeval: checkCeval,
    onJump: onJump,
    close: root.toggleRetro,
    trans: root.trans,
    turnColor: turnColor
  };
};
