var m = require('mithril');

module.exports = function(allow) {

  var storageKey = 'explorer-enabled';
  var allowed = m.prop(allow);
  var enabled = m.prop(allow && lichess.storage.get(storageKey) === '1');
  var loading = m.prop(true);

  var cache = {};

  function fetch(fen) {
    loading(true);
    m.request({
      method: 'GET',
      url: 'http://130.211.90.176/bullet?fen=' + fen
    }).then(function(data) {
      cache[fen] = data;
      loading(false);
    });
  }

  function setFen(fen) {
    if (!enabled()) return;
    if (!cache[fen]) fetch(fen);
  }

  return {
    allowed: allowed,
    enabled: enabled,
    loading: loading,
    setFen: setFen,
    get: function(fen) {
      return cache[fen];
    },
    toggle: function() {
      if (!allowed()) return;
      enabled(!enabled());
      lichess.storage.set(storageKey, enabled() ? '1' : '0');
    }
  };
};
