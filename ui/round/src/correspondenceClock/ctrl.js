var m = require('mithril');

module.exports = function(data, onFlag) {

  var lastUpdate;

  this.data = data;
  this.data.barTime = this.data.increment;

  function setLastUpdate() {
    lastUpdate = {
      white: data.white,
      black: data.black,
      at: new Date()
    };
  }
  setLastUpdate();

  this.update = function(white, black) {
    this.data.white = white;
    this.data.black = black;
    setLastUpdate();
  }.bind(this);

  this.tick = function(color) {
    m.startComputation();
    this.data[color] = Math.max(0, lastUpdate[color] - (new Date() - lastUpdate.at) / 1000);
    if (this.data[color] === 0) onFlag();
    m.endComputation();
  }.bind(this);
}
