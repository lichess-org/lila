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

  this.data = data;
  this.data.barTime = Math.max(this.data.initial, 2) + 5 * this.data.increment;

  function setLastUpdate() {
    lastUpdate = {
      white: data.white,
      black: data.black,
      at: new Date()
    };
  }
  setLastUpdate();

  this.update = function(white, black) {
    m.startComputation();
    this.data.white = white;
    this.data.black = black;
    setLastUpdate();
    m.endComputation();
  }.bind(this);

  this.tick = function(color) {
    this.data[color] = Math.max(0, lastUpdate[color] - (new Date() - lastUpdate.at) / 1000);
    if (this.data[color] === 0) onFlag();
    m.redraw();
    if (soundColor == color && this.data[soundColor] < this.data.emerg && emergSound.playable[soundColor]) {
      if (!emergSound.last || (data.increment && new Date() - emergSound.delay > emergSound.last)) {
        emergSound.play();
        emergSound.last = new Date();
        emergSound.playable[soundColor] = false;
      }
    } else if (soundColor == color && this.data[soundColor] > 2 * this.data.emerg && !emergSound.playable[soundColor]) {
      emergSound.playable[soundColor] = true;
    }
  }.bind(this);

  this.secondsOf = function(color) {
    return this.data[color];
  }.bind(this);
}
