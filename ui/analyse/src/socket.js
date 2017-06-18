var synthetic = require('./util').synthetic;
var initialBoardFen = require('chessground/fen').initial;

module.exports = function(send, ctrl) {

  this.send = send;

  var anaMoveTimeout;
  var anaDestsTimeout;

  var anaDestsCache;

  this.clearCache = function() {
    anaDestsCache = (
      ctrl.data.game.variant.key === 'standard' &&
        ctrl.tree.root.fen.split(' ', 1)[0] === initialBoardFen
    ) ? {
      '': {
        path: '',
        dests: 'iqy muC gvx ltB bqs pxF jrz nvD ksA owE'
      }
    } : {};
  };
  this.clearCache();

  // forecast mode: reload when opponent moves
  if (!synthetic(ctrl.data)) setTimeout(function() {
    send("startWatching", ctrl.data.game.id);
  }, 1000);

  var currentChapterId = function() {
    if (ctrl.study) return ctrl.study.vm.chapterId;
  };
  var addStudyData = function(req, isWrite) {
    var c = currentChapterId();
    if (c) {
      req.ch = c;
      if (isWrite) {
        if (ctrl.study.vm.mode.write) {
          if (!ctrl.study.vm.mode.sticky) req.sticky = false;
        }
        else req.write = false;
      }
    }
  };

  var handlers = {
    node: function(data) {
      clearTimeout(anaMoveTimeout);
      // no strict equality here!
      if (data.ch == currentChapterId())
        ctrl.addNode(data.node, data.path);
      else
      console.log('socket handler node got wrong chapter id', data);
    },
    stepFailure: function(data) {
      clearTimeout(anaMoveTimeout);
      ctrl.reset();
    },
    dests: function(data) {
      clearTimeout(anaDestsTimeout);
      if (!data.ch || data.ch === currentChapterId()) {
        anaDestsCache[data.path] = data;
        ctrl.addDests(data.dests, data.path, data.opening);
      } else
      console.log('socket handler node got wrong chapter id', data);
    },
    destsFailure: function(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    },
    fen: function(e) {
      if (ctrl.forecast && e.id === ctrl.data.game.id)
        ctrl.forecast.reloadToLastPly();
    },
    analysisProgress: function(data) {
      ctrl.mergeAnalysisData(data);
    },
    evalHit: function(e) {
      ctrl.evalCache.onCloudEval(e);
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
    addStudyData(req, true);
    this.send('anaMove', req);
    anaMoveTimeout = setTimeout(this.sendAnaMove.bind(this, req), 3000);
  }.bind(this);

  this.sendAnaDrop = function(req) {
    clearTimeout(anaMoveTimeout);
    withoutStandardVariant(req);
    addStudyData(req, true);
    this.send('anaDrop', req);
    anaMoveTimeout = setTimeout(this.sendAnaDrop.bind(this, req), 3000);
  }.bind(this);

  this.sendAnaDests = function(req) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(function() {
      handlers.dests(anaDestsCache[req.path]);
    }, 300);
    else {
      withoutStandardVariant(req);
      addStudyData(req);
      this.send('anaDests', req);
      anaDestsTimeout = setTimeout(function() {
        console.log(req, 'resendAnaDests');
        this.sendAnaDests(req);
      }.bind(this), 3000);
    }
  }.bind(this);

  this.sendForecasts = function(req) {
    this.send('forecasts', req);
  }.bind(this);

  var withoutStandardVariant = function(obj) {
    if (obj.variant === 'standard') delete obj.variant;
  };
}
