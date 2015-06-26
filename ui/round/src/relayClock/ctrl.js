var m = require('mithril');

module.exports = function(data) {

  var lastUpdate;

  this.data = data;

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
    m.redraw();
  }.bind(this);

  this.secondsOf = function(color) {
    return this.data[color];
  }.bind(this);
}
