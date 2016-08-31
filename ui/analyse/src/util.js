var piotr2key = require('./piotr').piotr2key;
var m = require('mithril');

var UNDEF = 'undefined';

var defined = function(v) {
  return typeof v !== UNDEF;
};

var plyToTurn = function(ply) {
  return Math.floor((ply - 1) / 2) + 1;
}

var fixCrazySan = function(san) {
  return san[0] === 'P' ? san.slice(1) : san;
}

module.exports = {
  initialFen: "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
  readDests: function(lines) {
    if (!defined(lines)) return null;
    var dests = {};
    if (lines) lines.split(' ').forEach(function(line) {
      dests[piotr2key[line[0]]] = line.split('').slice(1).map(function(c) {
        return piotr2key[c];
      });
    });
    return dests;
  },
  aiName: function(variant) {
    return variant.key === 'crazyhouse' ? 'Sunsetter' : 'Stockfish';
  },
  readDrops: function(line) {
    if (typeof line === 'undefined' || line === null) return null;
    return line.match(/.{2}/g) || [];
  },
  defined: defined,
  empty: function(a) {
    return !a || a.length === 0;
  },
  renderEval: function(e) {
    e = Math.max(Math.min(Math.round(e / 10) / 10, 99), -99);
    return (e > 0 ? '+' : '') + e;
  },
  synthetic: function(data) {
    return data.game.id === 'synthetic';
  },
  plyToTurn: plyToTurn,
  nodeFullName: function(node) {
    if (node.san) return plyToTurn(node.ply) + (
      node.ply % 2 === 1 ? '.' : '...'
    ) + ' ' + fixCrazySan(node.san);
    return 'Initial position';
  },
  fixCrazySan: fixCrazySan,
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
  decomposeUci: function(uci) {
    return [uci.slice(0, 2), uci.slice(2, 4), uci.slice(4, 5)];
  },
  median: function(values) {
    values.sort(function(a, b) {
      return a - b;
    });
    var half = Math.floor(values.length / 2);
    return values.length % 2 ? values[half] :
      (values[half - 1] + values[half]) / 2.0;
  },
  plural: function(noun, nb) {
    return nb + ' ' + (nb === 1 ? noun : noun + 's');
  },
  titleNameToId: function(titleName) {
    var split = titleName.split(' ');
    var name = split.length == 1 ? split[0] : split[1];
    return name.toLowerCase();
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
  roleToSan: {
    pawn: 'P',
    knight: 'N',
    bishop: 'B',
    rook: 'R',
    queen: 'Q'
  },
  sanToRole: {
    P: 'pawn',
    N: 'knight',
    B: 'bishop',
    R: 'rook',
    Q: 'queen'
  },
  /**
   * https://github.com/niksy/throttle-debounce/blob/master/throttle.js
   *
   * Throttle execution of a function. Especially useful for rate limiting
   * execution of handlers on events like resize and scroll.
   *
   * @param  {Number}    delay          A zero-or-greater delay in milliseconds. For event callbacks, values around 100 or 250 (or even higher) are most useful.
   * @param  {Boolean}   noTrailing     Optional, defaults to false. If noTrailing is true, callback will only execute every `delay` milliseconds while the
   *                                    throttled-function is being called. If noTrailing is false or unspecified, callback will be executed one final time
   *                                    after the last throttled-function call. (After the throttled-function has not been called for `delay` milliseconds,
   *                                    the internal counter is reset)
   * @param  {Function}  callback       A function to be executed after delay milliseconds. The `this` context and all arguments are passed through, as-is,
   *                                    to `callback` when the throttled-function is executed.
   * @param  {Boolean}   debounceMode   If `debounceMode` is true (at begin), schedule `clear` to execute after `delay` ms. If `debounceMode` is false (at end),
   *                                    schedule `callback` to execute after `delay` ms.
   *
   * @return {Function}  A new, throttled, function.
   */
  throttle: function(delay, noTrailing, callback, debounceMode) {

    // After wrapper has stopped being called, this timeout ensures that
    // `callback` is executed at the proper times in `throttle` and `end`
    // debounce modes.
    var timeoutID;

    // Keep track of the last time `callback` was executed.
    var lastExec = 0;

    // `noTrailing` defaults to falsy.
    if (typeof(noTrailing) !== 'boolean') {
      debounceMode = callback;
      callback = noTrailing;
      noTrailing = undefined;
    }

    // The `wrapper` function encapsulates all of the throttling / debouncing
    // functionality and when executed will limit the rate at which `callback`
    // is executed.
    return function() {

      var self = this;
      var elapsed = Number(new Date()) - lastExec;
      var args = arguments;

      // Execute `callback` and update the `lastExec` timestamp.
      function exec() {
        lastExec = Number(new Date());
        callback.apply(self, args);
      }

      // If `debounceMode` is true (at begin) this is used to clear the flag
      // to allow future `callback` executions.
      function clear() {
        timeoutID = undefined;
      }

      if (debounceMode && !timeoutID) {
        // Since `wrapper` is being called for the first time and
        // `debounceMode` is true (at begin), execute `callback`.
        exec();
      }

      // Clear any existing timeout.
      if (timeoutID) {
        clearTimeout(timeoutID);
      }

      if (debounceMode === undefined && elapsed > delay) {
        // In throttle mode, if `delay` time has been exceeded, execute
        // `callback`.
        exec();

      } else if (noTrailing !== true) {
        // In trailing throttle mode, since `delay` time has not been
        // exceeded, schedule `callback` to execute `delay` ms after most
        // recent execution.
        //
        // If `debounceMode` is true (at begin), schedule `clear` to execute
        // after `delay` ms.
        //
        // If `debounceMode` is false (at end), schedule `callback` to
        // execute after `delay` ms.
        timeoutID = setTimeout(debounceMode ? clear : exec, debounceMode === undefined ? delay - elapsed : delay);
      }

    };
  }
};
