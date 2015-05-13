var piotr2key = require('game').piotr.piotr2key;

module.exports = {
  readDests: function(lines) {
    if (typeof lines === 'undefined') return null;
    var dests = {};
    if (lines) lines.split(' ').forEach(function(line) {
      dests[piotr2key[line[0]]] = line.split('').slice(1).map(function(c) {
        return piotr2key[c];
      });
    });
    return dests;
  },
  defined: function(v) {
    return typeof v !== 'undefined';
  },
  empty: function(a) {
    return !a || a.length === 0;
  },
  // Returns a function, that, as long as it continues to be invoked, will not
  // be triggered. The function will be called after it stops being called for
  // N milliseconds. If `immediate` is passed, trigger the function on the
  // leading edge, instead of the trailing.
  debounce: function(func, wait, immediate) {
    var timeout;
    return function() {
      var context = this,
        args = arguments;
      var later = function() {
        timeout = null;
        if (!immediate) func.apply(context, args);
      };
      var callNow = immediate && !timeout;
      clearTimeout(timeout);
      timeout = setTimeout(later, wait);
      if (callNow) func.apply(context, args);
    };
  }
};
