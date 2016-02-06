var m = require('mithril');
var throttle = require('../util').throttle;

module.exports = function(allow) {

  var storageKey = 'explorer-enabled';
  var allowed = m.prop(allow);
  lichess.storage.set(storageKey, '1');
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');

  var cache = {};

  var fetch = throttle(500, false, function(fen) {
    m.request({
      method: 'GET',
      url: 'http://130.211.90.176/bullet?fen=' + fen
    }).then(function(data) {
      cache[fen] = data;
    });
  });

  var empty = {
    moves: {}
  };

  function setStep(step) {
    if (!enabled()) return;
    if (step.ply > 40) cache[step.fen] = empty;
    if (!cache[step.fen]) fetch(step.fen);
  }

  return {
    allowed: allowed,
    enabled: enabled,
    setStep: setStep,
    current: function(ctrl) {
      return cache[ctrl.vm.step.fen];
    },
    toggle: function() {
      if (!allowed()) return;
      enabled(!enabled());
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
    }
  };
};
