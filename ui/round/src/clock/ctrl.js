module.exports = function(data) {

  var lastUpdate;

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
    this.data.white = white;
    this.data.black = black;
    setLastUpdate();
  };

  this.tick = function(color) {
    m.startComputation();
    this.data[color] = lastUpdate[color] - (new Date() - lastUpdate.at) / 1000;
    m.endComputation();
  }.bind(this);
}
