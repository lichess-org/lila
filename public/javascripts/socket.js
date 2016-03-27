// ==ClosureCompiler==
// @compilation_level ADVANCED_OPTIMIZATIONS
// ==/ClosureCompiler==

var lichess = window.lichess = window.lichess || {};

lichess.StrongSocket = function(url, version, settings) {
  var self = this;
  self.settings = $.extend(true, {}, lichess.StrongSocket.defaults, settings);
  self.url = url;
  self.version = version;
  self.options = self.settings.options;
  self.ws = null;
  self.pingSchedule = null;
  self.connectSchedule = null;
  self.ackableMessages = [];
  self.lastPingTime = self.now();
  self.currentLag = 0;
  self.averageLag = 0;
  self.tryOtherUrl = false;
  self.autoReconnect = true;
  self.debug('Debug is enabled');
  if (self.options.resetUrl || self.options.prodPipe) {
    lichess.storage.remove(self.options.baseUrlKey);
  }
  if (self.options.prodPipe) {
    self.options.baseUrls = ['socket.en.lichess.org:9021'];
  }
  self.connect();
  window.addEventListener('unload', function() {
    self.destroy();
  });
};
lichess.StrongSocket.available = window.WebSocket || window.MozWebSocket;
lichess.StrongSocket.sri = Math.random().toString(36).substring(2);
lichess.StrongSocket.defaults = {
  events: {
    fen: function(e) {
      $('.live_' + e.id).each(function() {
        lichess.parseFen($(this).data("fen", e.fen).data("lastmove", e.lm));
      });
    },
    challenges: function(d) {
      lichess.challengeApp.update(d);
    }
  },
  params: {
    sri: lichess.StrongSocket.sri
  },
  options: {
    name: "unnamed",
    pingMaxLag: 8000, // time to wait for pong before reseting the connection
    pingDelay: 2000, // time between pong and ping
    autoReconnectDelay: 2000,
    lagTag: false, // jQuery object showing ping lag
    ignoreUnknownMessages: true,
    baseUrls: ['socket.' + document.domain].concat(
      /lichess\.org/.test(document.domain) ? [9021, 9022, 9023, 9024].map(function(port) {
        return 'socket.' + document.domain + ':' + port;
      }) : []),
    onFirstConnect: $.noop,
    baseUrlKey: 'surl3'
  }
};
lichess.StrongSocket.prototype = {
  connect: function() {
    var self = this;
    self.destroy();
    self.autoReconnect = true;
    var fullUrl = "ws://" + self.baseUrl() + self.url + "?" + $.param(self.settings.params);
    self.debug("connection attempt to " + fullUrl, true);
    try {
      if (window.MozWebSocket) self.ws = new MozWebSocket(fullUrl);
      else if (window.WebSocket) self.ws = new WebSocket(fullUrl);
      else throw "[lila] no websockets found on this browser!";

      self.ws.onerror = function(e) {
        self.onError(e);
      };
      self.ws.onclose = function(e) {
        if (self.autoReconnect) {
          self.debug('Will autoreconnect in ' + self.options.autoReconnectDelay);
          self.scheduleConnect(self.options.autoReconnectDelay);
        }
      };
      self.ws.onopen = function() {
        self.debug("connected to " + fullUrl, true);
        self.onSuccess();
        $('body').removeClass('offline');
        self.pingNow();
        $('body').trigger('socket.open');
        if ($('#user_tag').length) setTimeout(function() {
          self.send("following_onlines");
        }, 500);
        var resend = self.ackableMessages;
        self.ackableMessages = [];
        resend.forEach(function(x) {
          self.send(x.t, x.d);
        });
      };
      self.ws.onmessage = function(e) {
        var m = JSON.parse(e.data);
        // if (Math.random() > 0.5) {
        //   console.log(m, 'skip');
        //   return;
        // }
        if (m.t === 'n') self.pong();
        // else self.debug(e.data);
        if (m.t === 'b') m.d.forEach(function(mm) {
          self.handle(mm);
        });
        else self.handle(m);
      };
    } catch (e) {
      self.onError(e);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  send: function(t, d, o, again) {
    var self = this;
    var data = d || {},
      options = o || {};
    if (options && options.ackable)
      self.ackableMessages.push({
        t: t,
        d: d
      });
    var message = JSON.stringify({
      t: t,
      d: data
    });
    self.debug("send " + message);
    try {
      self.ws.send(message);
    } catch (e) {
      // maybe sent before socket opens,
      // try again a second later,once.
      if (!again) setTimeout(function() {
        this.send(t, d, o, true);
      }.bind(this), 1000);
    }
  },
  sendAckable: function(t, d) {
    this.send(t, d, {
      ackable: true
    });
  },
  scheduleConnect: function(delay) {
    var self = this;
    // self.debug('schedule connect ' + delay);
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    self.connectSchedule = setTimeout(function() {
      $('body').addClass('offline');
      self.tryOtherUrl = true;
      self.connect();
    }, delay);
  },
  schedulePing: function(delay) {
    var self = this;
    clearTimeout(self.pingSchedule);
    self.pingSchedule = setTimeout(function() {
      self.pingNow();
    }, delay);
  },
  pingNow: function() {
    var self = this;
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    try {
      self.ws.send(self.pingData());
      self.lastPingTime = self.now();
    } catch (e) {
      self.debug(e, true);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  pong: function() {
    var self = this;
    clearTimeout(self.connectSchedule);
    self.schedulePing(self.options.pingDelay);
    self.currentLag = self.now() - self.lastPingTime;
    if (!self.averageLag) self.averageLag = self.currentLag;
    else self.averageLag = 0.2 * (self.currentLag - self.averageLag) + self.averageLag;
    if (self.options.lagTag) {
      self.options.lagTag.html(Math.round(self.averageLag));
    }
  },
  pingData: function() {
    return JSON.stringify({
      t: "p",
      v: this.version
    });
  },
  handle: function(m) {
    var self = this;
    if (m.v) {
      if (m.v <= self.version) {
        self.debug("already has event " + m.v);
        return;
      }
      if (m.v > self.version + 1) {
        self.debug("event gap detected from " + self.version + " to " + m.v);
        if (!self.options.prodPipe) return;
      }
      self.version = m.v;
    }
    switch (m.t || false) {
      case false:
        break;
      case 'resync':
        if (!self.options.prodPipe) lichess.reload();
        break;
      case 'ack':
        self.ackableMessages = [];
        break;
      default:
        if (self.settings.receive) self.settings.receive(m.t, m.d);
        var h = self.settings.events[m.t];
        if (h) h(m.d || null, m);
        else if (!self.options.ignoreUnknownMessages) {
          self.debug('Message not supported ' + JSON.stringify(m));
        }
    }
  },
  now: function() {
    return new Date().getTime();
  },
  debug: function(msg, always) {
    if ((always || this.options.debug) && window.console && console.debug) {
      console.debug("[" + this.options.name + " " + this.settings.params.sri + "]", msg);
    }
  },
  destroy: function() {
    clearTimeout(this.pingSchedule);
    clearTimeout(this.connectSchedule);
    this.disconnect();
    this.ws = null;
  },
  disconnect: function() {
    if (this.ws) {
      this.debug("Disconnect", true);
      this.autoReconnect = false;
      this.ws.onerror = $.noop;
      this.ws.onclose = $.noop;
      this.ws.onopen = $.noop;
      this.ws.onmessage = $.noop;
      this.ws.close();
    }
  },
  onError: function(e) {
    var self = this;
    self.options.debug = true;
    self.debug('error: ' + JSON.stringify(e));
    self.tryOtherUrl = true;
    setTimeout(function() {
      if (!$('#network_error').length) {
        var msg = "Your browser supports websockets, but cannot get a connection. Maybe you are behind a proxy that does not support websockets. Ask your system administrator to fix it!";
        $('#top').append('<span class="fright link text" id="network_error" title="' + msg + '" data-icon="j">Network error</span>');
      }
    }, 1000);
    clearTimeout(self.pingSchedule);
  },
  onSuccess: function() {
    $('#network_error').remove();
    this.nbConnects = (this.nbConnects || 0) + 1;
    if (this.nbConnects === 1) this.options.onFirstConnect();
  },
  baseUrl: function() {
    var key = this.options.baseUrlKey;
    var urls = this.options.baseUrls;
    var url = lichess.storage.get(key);
    if (!url) {
      url = urls[0];
      lichess.storage.set(key, url);
    } else if (this.tryOtherUrl) {
      this.tryOtherUrl = false;
      url = urls[(urls.indexOf(url) + 1) % urls.length];
      lichess.storage.set(key, url);
    }
    return url;
  },
  pingInterval: function() {
    return this.options.pingDelay + this.averageLag;
  }
};
