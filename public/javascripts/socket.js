$.websocket = function(url, version, settings) {
  var self = this;
  self.settings = {
    events: {},
    params: {
      uid: Math.random().toString(36).substring(5) // 8 chars
    },
    options: {
      name: "unnamed",
      debug: false,
      offlineDelay: 5000, // time before showing offlineTag
      offlineTag: false, // jQuery object showing connection error
      pingMaxLag: 5000, // time to wait for pong before reseting the connection
      pingDelay: 1500, // time between pong and ping
      lagTag: false, // jQuery object showing ping lag
      ignoreUnknownMessages: false
    }
  };
  $.extend(true, self.settings, settings);
  self.url = url;
  self.version = version;
  self.options = self.settings.options;
  self.ws = null;
  self.fullUrl = null;
  self.pingSchedule = null;
  self.connectSchedule = null;
  self.lastPingTime = self.now();
  self.currentLag = 0;
  self.averageLag = 0;
  self.connect();
  $(window).unload(function() {
    self.destroy();
  });
}
$.websocket.available = window.WebSocket || window.MozWebSocket;
$.websocket.prototype = {
  connect: function() { var self = this;
    self.destroy();
    self.fullUrl = "ws://" + self.url + "?" + $.param($.extend(self.settings.params, { version: self.version }));
    self.debug("connection attempt to " + self.fullUrl);
    if (window.MozWebSocket) self.ws = new MozWebSocket(self.fullUrl);
    else if (window.WebSocket) self.ws = new WebSocket(self.fullUrl);
    else throw "no websockets found on this browser!";

    self.ws.onerror = self.onError;
    self.ws.onopen = function() {
      self.debug("connected to " + self.fullUrl);
      self.onSuccess();
      if (self.options.offlineTag) self.options.offlineTag.hide();
      self.pingNow();
      $('body').trigger('socket.open');
    };
    self.ws.onmessage = function(e) {
      var m = JSON.parse(e.data);
      if (m.t == "n") { self.pong(); }
      else self.debug(m);
      if (m.t == "b") {
        $(m.d || []).each(function() { self.handle(this); });
      } else {
        self.handle(m);
      }
    };
    self.scheduleConnect(self.options.pingMaxLag);
  },
  send: function(t, d) {
    var self = this;
    var data = d || {};
    var message = JSON.stringify({t: t, d: data});
    self.debug(message);
    self.ws.send(message);
  },
  scheduleConnect: function(delay) {
    var self = this;
    clearTimeout(self.connectSchedule);
    //self.debug("schedule connect in " + delay + " ms");
    self.connectSchedule = setTimeout(function() {
      if (self.options.offlineTag) self.options.offlineTag.show();
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
      //self.debug("ping " + self.pingData());
      self.ws.send(self.pingData());
      self.lastPingTime = self.now();
    } catch (e) {
      self.debug(e);
    }
    self.scheduleConnect(self.options.pingMaxLag);
  },
  pong: function() {
    var self = this;
    clearTimeout(self.connectSchedule);
    self.schedulePing(self.options.pingDelay);
    self.currentLag = self.now() - self.lastPingTime;
    if (self.options.lagTag) {
      self.options.lagTag.text(self.currentLag + " ms");
    }
    self.averageLag = self.averageLag * 0.8 + self.currentLag * 0.2;
  },
  pingData: function() {
    return JSON.stringify({t: "p", v: this.version});
  },
  handle: function(m) { var self = this;
    if (m.v) {
      if (m.v <= self.version) {
        self.debug("already has event " + m.v);
        return;
      }
      if (m.v > self.version + 1) {
        self.debug("event gap detected from " + self.version + " to " + m.v);
        return;
      }
      self.version = m.v;
      self.debug("set version " + self.version);
    }
    if (m.t) {
      if (m.t == "resync") {
        location.reload();
        return;
      }
      var h = self.settings.events[m.t];
      if ($.isFunction(h)) h(m.d || null);
      else if(!self.options.ignoreUnknownMessages) {
        self.debug(m.t + " not supported");
      }
    }
  },
  now: function() { return new Date().getTime(); },
  debug: function(msg) { if (this.options.debug) console.debug("[" + this.options.name + "]", msg); },
  destroy: function() {
    var self = this;
    clearTimeout(self.pingSchedule);
    clearTimeout(self.connectSchedule);
    if (self.ws) { self.ws.close(); self.ws = null; }
  },
  onError: function(e) {
    setTimeout(function() {
      if (!$.cookie("wsok") && $("#websocket-fail").length == 0) {
        $.ajax("/assets/websocket-fail.html", {
          success: function(html) {
            $('body').prepend("<div id='websocket-fail'>" + html + "</div>");
          }
        });
      }
    }, 1000);
  },
  onSuccess: function() {
    $.cookie("wsok", 1);
    $("#websocket-fail").remove();
  }
}
