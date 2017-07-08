export default function(opts) {

  var anaMoveTimeout;
  var anaDestsTimeout;

  var anaDestsCache = {};

  var handlers = {
    node: function(data) {
      clearTimeout(anaMoveTimeout);
      opts.addNode(data.node, data.path);
    },
    stepFailure: function() {
      clearTimeout(anaMoveTimeout);
      opts.reset();
    },
    dests: function(data) {
      anaDestsCache[data.path] = data;
      opts.addDests(data.dests, data.path, data.opening);
      clearTimeout(anaDestsTimeout);
    },
    destsFailure: function(data) {
      console.log(data);
      clearTimeout(anaDestsTimeout);
    }
  };

  var sendAnaMove = function(req) {
    clearTimeout(anaMoveTimeout);
    opts.send('anaMove', req);
    anaMoveTimeout = setTimeout(function() {
      sendAnaMove(req);
    }, 3000);
  };

  var sendAnaDests = function(req) {
    clearTimeout(anaDestsTimeout);
    if (anaDestsCache[req.path]) setTimeout(function() {
      handlers.dests(anaDestsCache[req.path]);
    }, 10);
    else {
      opts.send('anaDests', req);
      anaDestsTimeout = setTimeout(function() {
        sendAnaDests(req);
      }, 3000);
    }
  };

  return {
    send: opts.send,
    receive: function(type, data) {
      if (handlers[type]) {
        handlers[type](data);
        return true;
      }
      return false;
    },

    sendAnaMove: sendAnaMove,

    sendAnaDests: sendAnaDests
  };
}
