var m = require('mithril');

module.exports = function(data, onFlag, soundColor) {

  var lastUpdate;

  var emergSound = {
    play: $.sound.lowtime,
    last: null,
    delay: 20000,
    playable: {
      white: true,
      black: true
    }
  };

  data.barTime = Math.max(data.initial, 2) + 5 * data.increment;

  function setLastUpdate() {
    lastUpdate = {
      white: data.white,
      black: data.black,
      at: new Date()
    };
  }
  setLastUpdate();

  var update = function(white, black) {
    m.startComputation();
    data.white = white;
    data.black = black;
    setLastUpdate();
    m.endComputation();
  };

  var tick = function(color) {
    data[color] = Math.max(0, lastUpdate[color] - (new Date() - lastUpdate.at) / 1000);
    if (data[color] === 0) onFlag();
    m.redraw();
    if (soundColor == color && data[soundColor] < data.emerg && emergSound.playable[soundColor]) {
      if (!emergSound.last || (data.increment && new Date() - emergSound.delay > emergSound.last)) {
        emergSound.play();
        emergSound.last = new Date();
        emergSound.playable[soundColor] = false;
      }
    } else if (soundColor == color && data[soundColor] > 1.5 * data.emerg && !emergSound.playable[soundColor]) {
      emergSound.playable[soundColor] = true;
    }
  };

  var secondsOf = function(color) {
    return data[color];
  };

  return {
    data: data,
    update: update,
    tick: tick,
    secondsOf: secondsOf
  };
}
