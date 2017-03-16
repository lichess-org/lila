var game = require('game').game;

// Register blur events to be sent as move metadata

var lastFocus;
var lastMove;

var init = function(ctrl) {
  if (game.isPlayerPlaying(ctrl.data) && !ctrl.data.simul)
    window.addEventListener('focus', function() {
      lastFocus = Date.now();
    });
}

var get = function() {
  return lastFocus - lastMove > 1000;
};

var onMove = function() {
  lastMove = Date.now();
};

module.exports = {
  init: init,
  get: get,
  onMove: onMove
};
