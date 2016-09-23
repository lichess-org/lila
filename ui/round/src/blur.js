var game = require('game').game;

// Register blur events to be sent as move metadata

var blur = false;

var init = function(ctrl) {
  if (game.isPlayerPlaying(ctrl.data) && !ctrl.data.simul)
    window.addEventListener('blur', function() {
      blur = true;
    });
}

var get = function() {
  var value = blur;
  blur = false;
  return value;
};

var reset = function() {
  blur = false;
};

module.exports = {
  init: init,
  get: get,
  reset: reset
};
