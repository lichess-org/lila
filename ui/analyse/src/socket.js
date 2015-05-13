module.exports = function(send, ctrl) {

  this.send = send;

  var anaMoveTimeout;
  var anaDestsTimeout;

  var handlers = {
    step: function(data) {
      ctrl.addStep(data.step, data.path);
      clearTimeout(anaMoveTimeout);
    },
    stepFailure: function(data) {
      console.log(data);
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests: function(data) {
      ctrl.addDests(data.dests, data.path);
      clearTimeout(anaDestsTimeout);
    },
    destsFailure: function(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    }
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  this.sendAnaMove = function(req) {
    withoutStandardVariant(req);
    this.send('anaMove', req);
    anaMoveTimeout = setTimeout(this.sendAnaMove.bind(this, req), 3000);
  }.bind(this);

  this.sendAnaDests = function(req) {
    withoutStandardVariant(req);
    this.send('anaDests', req);
    anaDestsTimeout = setTimeout(this.sendAnaDests.bind(this, req), 3000);
  }.bind(this);

  var withoutStandardVariant = function(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  };
}
