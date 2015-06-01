module.exports = {
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
    var pms = {};
    if (possibleMoves) Object.keys(possibleMoves).forEach(function(k) {
      pms[k] = possibleMoves[k].match(/.{2}/g);
    });
    return pms;
  },
  throttle: function(callback, delay) {
    var timeoutId;
    return function() {
      if (timeoutId) clearTimeout(timeoutId);
      var args = arguments;
      timeoutId = setTimeout(function() {
        callback.apply(null, args);
      }, delay);
    };
  }
};
