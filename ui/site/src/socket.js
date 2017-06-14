var makeAckable = require('./ackable');

// versioned events, acks, retries, resync
lichess.StrongSocket = function(url, version, settings) {

  var now = Date.now;

  var settings = $.extend(true, {}, lichess.StrongSocket.defaults, settings);
  var url = url;
  var version = version;
  var versioned = version !== false;
  var options = settings.options;
  var ws = null;
  var pingSchedule = null;
  var connectSchedule = null;
  var ackable = makeAckable(function(t, d) { send(t, d) });
  var lastPingTime = now();
  var currentLag = 0;
  var averageLag = 0;
  var tryOtherUrl = false;
  var autoReconnect = true;
  var nbConnects = 0;
  var storage = lichess.storage.make(options.baseUrlKey);
  if (options.resetUrl) storage.remove();

  var connect = function() {
    destroy();
    autoReconnect = true;
    var fullUrl = options.protocol + "//" + baseUrl() + url + "?" + $.param(settings.params);
    debug("connection attempt to " + fullUrl);
    try {
      ws = new WebSocket(fullUrl);
      ws.onerror = function(e) {
        onError(e);
      };
      ws.onclose = function(e) {
        if (autoReconnect) {
          debug('Will autoreconnect in ' + options.autoReconnectDelay);
          scheduleConnect(options.autoReconnectDelay);
        }
      };
      ws.onopen = function() {
        debug("connected to " + fullUrl);
        onSuccess();
        $('body').removeClass('offline');
        pingNow();
        lichess.pubsub.emit('socket.open')();
        ackable.resend();
      };
      ws.onmessage = function(e) {
        var m = JSON.parse(e.data);
        // if (Math.random() > 0.5) {
        //   console.log(m, 'skip');
        //   return;
        // }
        if (m.t === 'n') pong();
        // else debug(e.data);
        if (m.t === 'b') m.d.forEach(handle);
        else handle(m);
      };
    } catch (e) {
      onError(e);
    }
    scheduleConnect(options.pingMaxLag);
  };

  var send = function(t, d, o, noRetry) {
    o = o || {};
    var msg = { t: t };
    if (d !== undefined) {
      if (o.millis !== undefined) d.s = Math.round(o.millis * 0.1).toString(36);
      msg.d = d;
    }
    if (o.ackable) ackable.register(t, d);
    var message = JSON.stringify(msg);
    debug("send " + message);
    try {
      ws.send(message);
    } catch (e) {
      // maybe sent before socket opens,
      // try again a second later.
      if (!noRetry) setTimeout(function() {
        send(t, d, o, true);
      }, 1000);
    }
  };
  lichess.pubsub.on('socket.send', send);

  var scheduleConnect = function(delay) {
    if (options.idle) delay = 10 * 1000 + Math.random() * 10 * 1000;
    // debug('schedule connect ' + delay);
    clearTimeout(pingSchedule);
    clearTimeout(connectSchedule);
    connectSchedule = setTimeout(function() {
      $('body').addClass('offline');
      tryOtherUrl = true;
      connect();
    }, delay);
  };

  var schedulePing = function(delay) {
    clearTimeout(pingSchedule);
    pingSchedule = setTimeout(pingNow, delay);
  };

  var pingNow = function() {
    clearTimeout(pingSchedule);
    clearTimeout(connectSchedule);
    try {
      ws.send(pingData());
      lastPingTime = now();
    } catch (e) {
      debug(e, true);
    }
    scheduleConnect(options.pingMaxLag);
  };

  var computePingDelay = function() {
    return options.pingDelay + (options.idle ? 1000 : 0);
  };

  var pong = function() {
    clearTimeout(connectSchedule);
    schedulePing(computePingDelay());
    currentLag = now() - lastPingTime;
    if (!averageLag) averageLag = currentLag;
    else averageLag = 0.2 * (currentLag - averageLag) + averageLag;
    lichess.pubsub.emit('socket.lag')(averageLag);
  };

  var pingData = function() {
    return versioned ? JSON.stringify({
      t: "p",
      v: version
    }) : '{"t":"p"}';
  };

  var handle = function(m) {
    if (m.v) {
      if (m.v <= version) {
        debug("already has event " + m.v);
        return;
      }
      if (m.v > version + 1) {
        debug("event gap detected from " + version + " to " + m.v);
        return;
      }
      version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        lichess.reload();
        break;
      case 'ack':
        ackable.gotAck();
        break;
      default:
        lichess.pubsub.emit('socket.in.' + m.t)(m.d);
        var processed = settings.receive && settings.receive(m.t, m.d);
        if (!processed && settings.events[m.t]) settings.events[m.t](m.d || null, m);
    }
  };

  var debug = function(msg, always) {
    if (always || options.debug) {
      console.debug("[" + options.name + " " + settings.params.sri + "]", msg);
    }
  };

  var destroy = function() {
    clearTimeout(pingSchedule);
    clearTimeout(connectSchedule);
    disconnect();
    ws = null;
  };

  var disconnect = function(onNextConnect) {
    if (ws) {
      debug("Disconnect");
      autoReconnect = false;
      ws.onerror = ws.onclose = ws.onopen = ws.onmessage = $.noop;
      ws.close();
    }
    if (onNextConnect) options.onNextConnect = onNextConnect;
  };

  var onError = function(e) {
    options.debug = true;
    debug('error: ' + JSON.stringify(e));
    tryOtherUrl = true;
    setTimeout(function() {
      if (!$('#network_error').length) {
        $('#top').append('<span class="link text" id="network_error" data-icon="j">Network error</span>');
      }
    }, 1000);
    clearTimeout(pingSchedule);
  };

  var onSuccess = function() {
    $('#network_error').remove();
    nbConnects++;
    if (nbConnects === 1) {
      options.onFirstConnect();
      var disconnectTimeout;
      lichess.idleTimer(10 * 60 * 1000, function() {
        options.idle = true;
        disconnectTimeout = setTimeout(lichess.socket.destroy, 2 * 60 * 60 * 1000);
      }, function() {
        options.idle = false;
        if (ws) clearTimeout(disconnectTimeout);
        else location.reload();
      });
    }
    if (options.onNextConnect) {
      options.onNextConnect();
      delete options.onNextConnect;
    }
  };

  var baseUrl = function() {
    var urls = options.baseUrls;
    var url = storage.get();
    if (!url) {
      url = urls[0];
      storage.set(url);
    } else if (tryOtherUrl) {
      tryOtherUrl = false;
      url = urls[(urls.indexOf(url) + 1) % urls.length];
      storage.set(url);
    }
    return url;
  };

  connect();
  window.addEventListener('unload', destroy);

  return {
    connect: connect,
    disconnect: disconnect,
    send: send,
    destroy: destroy,
    options: options,
    pingInterval: function() {
      return computePingDelay() + averageLag;
    },
    averageLag: function() {
      return averageLag;
    },
    getVersion: function() {
      return version;
    }
  };
};
lichess.StrongSocket.sri = Math.random().toString(36).substring(2).slice(0, 10);
lichess.StrongSocket.available = window.WebSocket || window.MozWebSocket;
lichess.StrongSocket.defaults = {
  events: {
    fen: function(e) {
      $('.live_' + e.id).each(function() {
        lichess.parseFen($(this).data("fen", e.fen).data("lastmove", e.lm));
      });
    },
    challenges: function(d) {
      lichess.challengeApp.update(d);
    },
    notifications: function(d) {
      lichess.notifyApp.update(d, true);
    }
  },
  params: {
    sri: lichess.StrongSocket.sri
  },
  options: {
    name: "unnamed",
    idle: false,
    pingMaxLag: 8000, // time to wait for pong before reseting the connection
    pingDelay: 2000, // time between pong and ping
    autoReconnectDelay: 2000,
    protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
    baseUrls: (function(domain) {
      var main = 'socket.' + domain;
      var isProduction = /lichess\.org/.test(main);
      var extraPorts = [];
      if (isProduction) {
        if (location.protocol === 'https:') extraPorts = [9025, 9026, 9027, 9028, 9029];
        else extraPorts = [9021, 9022, 9023, 9024];
      }
      return [main].concat(extraPorts.map(function(port) {
        return main + ':' + port;
      }));
    })(document.domain),
    onFirstConnect: $.noop,
    baseUrlKey: 'surl5'
  }
};
