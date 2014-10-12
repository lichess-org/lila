var round = require('./round');

// Register blur events to be sent as move metadata

var blur = false;

var init = function(ctrl) {
  if (round.isPlayerPlaying(ctrl.data)) {
    window.addEventListener('blur', function() {
      blur = true;
    });
  }
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
