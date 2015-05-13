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
  throttle: function(callback, limit) {
    var wait = false; // Initially, we're not waiting
    return function() { // We return a throttled function
      if (!wait) { // If we're not waiting
        callback.call(); // Execute users function
        wait = true; // Prevent future invocations
        setTimeout(function() { // After a period of time
          wait = false; // And allow future invocations
        }, limit);
      }
    }
  }
};
