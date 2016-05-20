var util = require('./util');

module.exports = function(send, ctrl) {

  this.send = send;

  var anaMoveTimeout;
  var anaDestsTimeout;

  var anaDestsCache = ctrl.data.game.variant.key === 'standard' ? {
    '': {
      path: '',
      dests: 'iqy muC gvx ltB bqs pxF jrz nvD ksA owE'
    }
  } : {};

  if (!util.synthetic(ctrl.data)) setTimeout(function() {
    send("startWatching", ctrl.data.game.id);
  }, 1000);

  var handlers = {
    node: function(data) {
      clearTimeout(anaMoveTimeout);
      ctrl.addNode(data.node, data.path);
    },
    stepFailure: function(data) {
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests: function(data) {
      anaDestsCache[data.path] = data;
      ctrl.addDests(data.dests, data.path, data.opening);
      clearTimeout(anaDestsTimeout);
    },
    destsFailure: function(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    },
    fen: function(e) {
      if (ctrl.forecast && e.id === ctrl.data.game.id)
        ctrl.forecast.reloadToLastPly();
    }
  };

  this.receive = function(type, data) {
    if (handlers[type]) {
      handlers[type](data);
      return true;
    } else if (ctrl.study && ctrl.study.socketHandlers[type]) {
      ctrl.study.socketHandlers[type](data);
      return true;
    }
    return false;
  }.bind(this);

  this.sendAnaMove = function(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    if (ctrl.study) ctrl.study.anaMoveConfig(req);
    this.send('anaMove', req);
    anaMoveTimeout = setTimeout(this.sendAnaMove.bind(this, req), 3000);
  }.bind(this);

  this.sendAnaDrop = function(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    if (ctrl.study) ctrl.study.anaMoveConfig(req);
    this.send('anaDrop', req);
    anaMoveTimeout = setTimeout(this.sendAnaDrop.bind(this, req), 3000);
  }.bind(this);

  this.sendAnaDests = function(req) {
    clearTimeout(anaDestsTimeout);
    withoutStandardVariant(req);
    if (anaDestsCache[req.path]) setTimeout(function() {
      handlers.dests(anaDestsCache[req.path]);
    }, 100);
    else {
      this.send('anaDests', req);
      anaDestsTimeout = setTimeout(this.sendAnaDests.bind(this, req), 3000);
    }
  }.bind(this);

  this.sendForecasts = function(req) {
    this.send('forecasts', req);
  }.bind(this);

  var withoutStandardVariant = function(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  };
}
