var m = require('mithril');

module.exports = {
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
  visible: (function() {
    var stateKey, eventKey, keys = {
      hidden: "visibilitychange",
      webkitHidden: "webkitvisibilitychange",
      mozHidden: "mozvisibilitychange",
      msHidden: "msvisibilitychange"
    };
    for (stateKey in keys) {
      if (stateKey in document) {
        eventKey = keys[stateKey];
        break;
      }
    }
    return function(c) {
      if (c) document.addEventListener(eventKey, c);
      return !document[stateKey];
    }
  })(),
  parsePossibleMoves: function(possibleMoves) {
    if (!possibleMoves) return {};
    for (var k in possibleMoves) {
      if (typeof possibleMoves[k] === 'object') break;
      possibleMoves[k] = possibleMoves[k].match(/.{2}/g);
    }
    return possibleMoves;
  }
};
