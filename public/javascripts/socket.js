// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

// versioned events, acks, retries, resync
lichess.StrongSocket = function(url, version, settings) {

  var now = function() {
    return new Date().getTime();
  };

  var settings = $.extend(true, {}, lichess.StrongSocket.defaults, settings);
  var url = url;
  var version = version;
  var options = settings.options;
  var ws = null;
  var pingSchedule = null;
  var connectSchedule = null;
  var ackableMessages = [];
  var lastPingTime = now();
  var currentLag = 0;
  var averageLag = 0;
  var tryOtherUrl = false;
  var autoReconnect = true;
  var nbConnects = 0;
  if (options.resetUrl || options.prodPipe) lichess.storage.remove(options.baseUrlKey);
  if (options.prodPipe) options.baseUrls = ['socket.lichess.org'];

  var connect = function() {
    destroy();
    autoReconnect = true;
    var fullUrl = options.protocol + "//" + baseUrl() + url + "?" + $.param(settings.params);
    debug("connection attempt to " + fullUrl, true);
    try {
      if (window.MozWebSocket) ws = new MozWebSocket(fullUrl);
      else if (window.WebSocket) ws = new WebSocket(fullUrl);
      else throw "[lila] no websockets found on this browser!";

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
        debug("connected to " + fullUrl, true);
        onSuccess();
        $('body').removeClass('offline');
        pingNow();
        lichess.pubsub.emit('socket.open')();
        var resend = ackableMessages;
        ackableMessages = [];
        resend.forEach(function(x) {
          send(x.t, x.d);
        });
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

  var send = function(t, d, o, again) {
    var data = d || {},
      options = o || {};
    if (options.withLag) d.l = Math.round(averageLag);
    if (options.ackable) ackableMessages.push({
      t: t,
      d: d
    });
    var message = JSON.stringify({
      t: t,
      d: data
    });
    debug("send " + message);
    try {
      ws.send(message);
    } catch (e) {
      // maybe sent before socket opens,
      // try again a second later,once.
      if (!again) setTimeout(function() {
        send(t, d, o, true);
      }, 1000);
    }
  };
  lichess.pubsub.on('socket.send', send);

  var sendAckable = function(t, d) {
    send(t, d, {
      ackable: true
    });
  };

  var scheduleConnect = function(delay) {
    if (options.idle) delay = delay * 3;
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
    if (options.lagTag) options.lagTag.html(Math.round(averageLag));
  };

  var pingData = function() {
    return JSON.stringify({
      t: "p",
      v: version
    });
  };

  var handle = function(m) {
    if (m.v) {
      if (m.v <= version) {
        debug("already has event " + m.v);
        return;
      }
      if (m.v > version + 1) {
        debug("event gap detected from " + version + " to " + m.v);
        if (!options.prodPipe) return;
      }
      version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        if (!options.prodPipe) lichess.reload();
        break;
      case 'ack':
        ackableMessages = [];
        break;
      default:
        lichess.pubsub.emit('socket.in.' + m.t)(m.d);
        if (settings.receive) settings.receive(m.t, m.d);
        var h = settings.events[m.t];
        if (h) h(m.d || null, m);
    }
  };

  var debug = function(msg, always) {
    if ((always || options.debug) && window.console && console.debug) {
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
      debug("Disconnect", true);
      autoReconnect = false;
      ws.onerror = $.noop;
      ws.onclose = $.noop;
      ws.onopen = $.noop;
      ws.onmessage = $.noop;
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
        var msg = "Your browser supports websockets, but cannot get a connection. Maybe you are behind a proxy that does not support websockets. Ask your system administrator to fix it!";
        $('#top').append('<span class="fright link text" id="network_error" title="' + msg + '" data-icon="j">Network error</span>');
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
    if (lichess.proxy) lichess.proxy.getLatency(options.protocol + "//" + baseUrl(), function(ms) {
      console.log(ms, 'latency');
    });
  };

  var baseUrl = function() {
    var key = options.baseUrlKey;
    var urls = options.baseUrls;
    var url = lichess.storage.get(key);
    if (!url) {
      url = urls[0];
      lichess.storage.set(key, url);
    } else if (tryOtherUrl) {
      tryOtherUrl = false;
      url = urls[(urls.indexOf(url) + 1) % urls.length];
      lichess.storage.set(key, url);
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
    lagTag: false, // jQuery object showing ping lag
    protocol: location.protocol === 'https:' ? 'wss:' : 'ws:',
    baseUrls: (function(domain) {
      var main = 'socket.' + domain.split('.').slice(1).join('.');
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
