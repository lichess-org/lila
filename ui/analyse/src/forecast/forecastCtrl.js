var m = require('mithril');

var partial = require('chessground').util.partial;

module.exports = function(cfg, saveUrl) {

  var forecasts = cfg.steps || [];
  var loading = m.prop(false);

  var keyOf = function(fc) {
    return fc.map(function(step) {
      return step.ply + ':' + step.uci;
    }).join(',');
  };

  var contains = function(fc1, fc2) {
    return fc1.length >= fc2.length && keyOf(fc1).indexOf(keyOf(fc2)) === 0;
  };

  var findStartingWithStep = function(step) {
    return forecasts.filter(function(fc) {
      return contains(fc, [step]);
    });
  };

  var collides = function(fc1, fc2) {
    var res = (function() {
      for (var i = 0, max = Math.min(fc1.length, fc2.length); i < max; i++) {
        if (fc1[i].uci !== fc2[i].uci) {
          if (cfg.onMyTurn) return i !== 0 && i % 2 === 0;
          return i % 2 === 1;
        }
      }
      return true;
    })();
    return res;
  };

  var truncate = function(fc) {
    if (cfg.onMyTurn)
      return (fc.length % 2 !== 1 ? fc.slice(0, -1) : fc).slice(0, 30);
    // must end with player move
    return (fc.length % 2 !== 0 ? fc.slice(0, -1) : fc).slice(0, 30);
  };

  var isLongEnough = function(fc) {
    return fc.length >= (cfg.onMyTurn ? 1 : 2);
  };

  var fixAll = function() {
    // remove contained forecasts
    forecasts = forecasts.filter(function(fc, i) {
      return forecasts.filter(function(f, j) {
        return i !== j && contains(f, fc)
      }).length === 0;
    });
    // remove colliding forecasts
    forecasts = forecasts.filter(function(fc, i) {
      return forecasts.filter(function(f, j) {
        return i < j && collides(f, fc)
      }).length === 0;
    });
  };
  fixAll();

  var reloadToLastPly = function() {
    loading(true);
    m.redraw();
    if (window.history.replaceState) window.history.replaceState(null, null, '#last');
    lichess.reload();
  };

  var isCandidate = function(fc) {
    fc = truncate(fc);
    if (!isLongEnough(fc)) return false;
    var collisions = forecasts.filter(function(f) {
      return contains(f, fc);
    });
    if (collisions.length) return false;
    return true;
  };

  var save = function() {
    if (cfg.onMyTurn) return;
    loading(true);
    m.redraw();
    m.request({
      method: 'POST',
      url: saveUrl,
      data: forecasts
    }).then(function(data) {
      if (data.reload) reloadToLastPly();
      else {
        loading(false);
        forecasts = data.steps || [];
      }
    });
  };

  var playAndSave = function(step) {
    if (!cfg.onMyTurn) return;
    loading(true);
    m.redraw();
    m.request({
      method: 'POST',
      url: saveUrl + '/' + step.uci,
      data: findStartingWithStep(step).filter(function(fc) {
        return fc.length > 1;
      }).map(function(fc) {
        return fc.slice(1);
      })
    }).then(function(data) {
      if (data.reload) reloadToLastPly();
      else {
        loading(false);
        forecasts = data.steps || [];
      }
    });
  };

  return {
    addSteps: function(fc) {
      fc = truncate(fc);
      if (!isCandidate(fc)) return;
      fc.forEach(function(step) {
        delete step.variations;
      });
      forecasts.push(fc);
      fixAll();
      save();
    },
    isCandidate: isCandidate,
    removeIndex: function(index) {
      forecasts = forecasts.filter(function(fc, i) {
        return i !== index;
      });
      save();
    },
    list: function() {
      return forecasts;
    },
    truncate: truncate,
    loading: loading,
    onMyTurn: cfg.onMyTurn,
    findStartingWithStep: findStartingWithStep,
    playAndSave: playAndSave,
    reloadToLastPly: reloadToLastPly
  };
};
