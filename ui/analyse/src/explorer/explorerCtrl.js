var m = require('mithril');
var throttle = require('../util').throttle;

module.exports = function(allow) {

  var storageKey = 'explorer-enabled';
  var allowed = m.prop(allow);
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

  function setFen(fen) {
    if (!enabled()) return;
    if (!cache[fen]) fetch(fen);
  }

  return {
    allowed: allowed,
    enabled: enabled,
    setFen: setFen,
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
