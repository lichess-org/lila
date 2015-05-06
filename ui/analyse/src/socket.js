module.exports = function(send, ctrl) {

  this.send = send;

  var anaMoveTimeout;

  var handlers = {
    step: function(data) {
      ctrl.addStep(data.step, data.path);
      clearTimeout(anaMoveTimeout);
    },
    stepFailure: function(data) {
      console.log(data);
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    }
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  this.sendAnaMove = function(move) {
    this.send('anaMove', move);
    anaMoveTimeout = setTimeout(this.sendAnaMove.bind(this, move), 3000);
  }.bind(this);
}
