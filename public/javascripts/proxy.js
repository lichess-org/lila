lichess.proxy = (function() {

  var protocol = location.protocol === 'https:' ? 'wss:' : 'ws:';
  var route = '/socket/ping';

  var pings = 3;
  var totalTimeout = 3000;

  var getLatency = function(host, callback) {
    var fullUrl = host + route;
    var ws = new WebSocket(fullUrl);
    var it = 0;
    var startAt;
    ws.onopen = function() {
      startAt = new Date();
      ws.send(pings - 1);
    };
    ws.onmessage = function(m) {
      if (!timeout) return;
      it++;
      if (it >= pings) {
        clearTimeout(timeout);
        ws.close();
        var time = new Date() - startAt;
        callback(time / pings);
      } else ws.send(pings - it - 1);
    };
    // ws.onclose = function() {
    //   console.log(fullUrl, 'closed');
    // };
    var timeout = setTimeout(function() {
      callback(totalTimeout);
      ws.close();
    }, totalTimeout);
  };

  return {
    getLatency: getLatency
  }
})();
