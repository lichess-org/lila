var m = require('mithril');

var UNDEF = 'undefined';

var defined = function(v) {
  return typeof v !== UNDEF;
};

module.exports = {
  defined: defined,
  empty: function(a) {
    return !a || a.length === 0;
  },
  classSet: function(classes) {
    var arr = [];
    for (var i in classes) {
      if (classes[i]) arr.push(i);
    }
    return arr.join(' ');
  },
  storedProp: function(k, defaultValue) {
    var sk = 'analyse.' + k;
    var value;
    var isBoolean = defaultValue === true || defaultValue === false;
    return function(v) {
      if (defined(v) && v != value) {
        value = v + '';
        lichess.storage.set(sk, v);
      } else if (!defined(value)) {
        value = lichess.storage.get(sk);
        if (value === null) value = defaultValue + '';
      }
      return isBoolean ? value === 'true' : value;
    };
  },
  storedJsonProp: function(keySuffix, defaultValue) {
    var key = 'explorer.' + keySuffix;
    return function() {
      if (arguments.length) lichess.storage.set(key, JSON.stringify(arguments[0]));
      var ret = JSON.parse(lichess.storage.get(key));
      return (ret !== null) ? ret : defaultValue;
    };
  },
  bindOnce: function(eventName, f) {
    var withRedraw = function(e) {
      m.startComputation();
      f(e);
      m.endComputation();
    };
    return function(el, isUpdate, ctx) {
      if (isUpdate) return;
      el.addEventListener(eventName, withRedraw)
      ctx.onunload = function() {
        el.removeEventListener(eventName, withRedraw);
      };
    }
  },
  throttle: require('./throttle'),
  dropThrottle: function(delay) {
    var task;
    var run = function(f) {
      task = f;
      f();
      setTimeout(function() {
        if (task !== f) run(task);
        else task = undefined;
      }, delay);
    };
    return function(f) {
      if (task) task = f;
      else run(f);
    };
  }
};
