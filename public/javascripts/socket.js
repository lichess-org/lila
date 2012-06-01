$.websocket = function(url, version, settings) {
  this.settings = {
    open: function(){},
    close: function(){},
    message: function(m){},
    events: {},
    params: {
      uid: Math.random().toString(36).substring(5)
    },
    options: {
      name: "unnamed",
      debug: false,
      offlineDelay: 5000,
      offlineTag: false,
      pingData: JSON.stringify({t: "p"}),
      pingTimeout: 5000,
      pingDelay: 1500
    }
  };
  $.extend(true, this.settings, settings);
  this.url = url;
  this.version = version;
  this.options = this.settings.options;
  this.ws = null;
  this.fullUrl = null;
  this.offlineTimeout = null;
  this.pingTimeout = null;
  this.connect();
  $(window).unload(this._destroy);
}
$.websocket.available = window.WebSocket || window.MozWebSocket;
$.websocket.prototype = {
  connect: function() { var self = this;
    self._destroy();
    self.fullUrl = "ws://" + self.url + "?" + $.param($.extend(self.settings.params, { version: self.version }));
    self._debug("connection attempt to " + self.fullUrl);
    if (window.MozWebSocket) self.ws = new MozWebSocket(self.fullUrl);
    else if (window.WebSocket) self.ws = new WebSocket(self.fullUrl);
    else self.ws = {
      send: function(m){ return false },
        close: function(){}
    };
    $(self.ws)
      .bind('open', function() {
        self._debug("connected to " + self.fullUrl);
        if (self.offlineTimeout) clearTimeout(self.offlineTimeout);
        if (self.options.offlineTag) self.options.offlineTag.hide();
        self._keepAlive();
        self.settings.open();
      })
    .bind('close', function() {
      self._debug("disconnected");
      if (self.options.offlineDelay && !self.offlineTimeout) self.offlineTimeout = setTimeout(function() {
        if (self.options.offlineTag) self.options.offlineTag.show();
      }, self.options.offlineDelay);
      self.settings.close();
    })
    .bind('message', function(e){
      var m = JSON.parse(e.originalEvent.data);
      if (m.t == "n") {
        setTimeout(function() { self._keepAlive(); }, self.options.pingDelay);
      }
      self._debug(m);
      if (m.t == "batch") {
        $(m.d || []).each(function() { self._handle(this); });
      } else {
        self._handle(m);
      }
    });
  },
  _keepAlive: function() {
    var self = this;
    clearTimeout(self.pingTimeout);
    try {
      self.ws.send(self.options.pingData);
      self.pingTimeout = setTimeout(function() {
        self._debug("reconnect!");
        self.connect();
      }, self.options.pingTimeout);
    } catch (e) {
      throw e;
      self._debug(e);
      self.connect();
    }
  },
  send: function(t, d) {
    var data = d || {};
    this._debug({t: t, d: data});
    return this.ws.send(JSON.stringify({t: t, d: data}));
  },
  disconnect: function() {
    this.ws.close();
  },
  _handle: function(m) { var self = this;
    if (m.v) self.version = m.v;
    var h = self.settings.events[m.t];
    if ($.isFunction(h)) h(m.d || null);
    else self._debug(m.t + " not supported");
    self.settings.message(m);
  },
  _debug: function(msg) { if (this.options.debug) console.debug("[" + this.options.name + "]", msg); },
  _destroy: function() { if (this.ws) { this.disconnect(); this.ws = null; } }
}
