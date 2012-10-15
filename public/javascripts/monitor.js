(function (app) {
  
//////////////////
// websocket.js //
//////////////////

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
};

  function create(elt) { return window.document.createElement(elt); }

  function SpeedOMeter (config) {
    this.maxVal = config.maxVal;
    this.threshold = config.threshold || 1;
    this.unit = config.unit ? config.unit + " " : "";
    this.name = config.name;
    this.container = config.container;
    this.elt = create("div");
    this.elt.className = "monitor";

    var title = create("span");
    title.innerHTML = this.name;
    title.className = 'title';
    this.elt.appendChild(title);

    this.screenCurrent = create("span");
    this.screenCurrent.className = 'screen current';
    this.elt.appendChild(this.screenCurrent);

    this.screenMax = create("span");
    this.screenMax.className = 'screen max';
    this.screenMax.innerHTML = this.maxVal + this.unit;
    this.elt.appendChild(this.screenMax);

    this.needle = create("div");
    this.needle.className = "needle";
    this.elt.appendChild(this.needle);

    this.light = create("div");
    this.light.className = "light";
    this.elt.appendChild(this.light);

    var wheel = create("div");
    wheel.className = "wheel";
    this.elt.appendChild(wheel);

    this.container.appendChild(this.elt);
  }

  SpeedOMeter.prototype.update = function (val) {
    Zanimo.transition(
        this.needle,
        "transform",
        "rotate(" + (val > this.maxVal ? 175 : val * 170 / this.maxVal) + "deg)",
        1500,
        "ease-in"
        );
    if (val > (this.threshold * this.maxVal)) {
      this.elt.className = "monitor alert";
    } else {
      this.elt.className = "monitor";
    }
    this.screenCurrent.innerHTML = val + this.unit;
  }

  function init() {

    var container = window.document.getElementById("monitors")

    var app = {};

    app.rps = new SpeedOMeter({
      name : "RPS",
      maxVal : 100,
      threshold: 0.9,
      container : container
    });

    app.memory = new SpeedOMeter({
      name : "MEMORY",
      maxVal : 3000,
      unit : "MB",
      container : container
    });

    app.cpu = new SpeedOMeter({
      name : "CPU",
      maxVal : 100,
      threshold: 0.3,
      unit : "%",
      container : container
    });

    app.thread = new SpeedOMeter({
      name : "THREAD",
      maxVal : 300,
      threshold: 0.8,
      container : container
    });

    app.load = new SpeedOMeter({
      name : "LOAD",
      maxVal : 1,
      threshold: 0.5,
      container : container
    });

    app.lat = new SpeedOMeter({
      name : "LATENCY",
      maxVal : 5,
      container : container
    });

    app.users = new SpeedOMeter({
      name : "USERS",
      maxVal : 500,
      container : container
    });

    app.lobby = new SpeedOMeter({
      name : "LOBBY",
      maxVal : 100,
      threshold: 1,
      container : container
    });

    app.game = new SpeedOMeter({
      name : "GAME",
      maxVal : 300,
      threshold: 1,
      container : container
    });

    app.dbMemory = new SpeedOMeter({
      name : "DB MEMORY",
      maxVal : 2000,
      threshold: 0.8,
      container : container
    });

    app.dbConn = new SpeedOMeter({
      name : "DB CONN",
      maxVal : 300,
      threshold: 0.8,
      container : container
    });

    app.dbQps = new SpeedOMeter({
      name : "DB QPS",
      maxVal : 300,
      threshold: 0.8,
      container : container
    });

    app.dbLock = new SpeedOMeter({
      name : "DB LOCK",
      maxVal : 1,
      container : container
    });

    app.ai = new SpeedOMeter({
      name : "AI PING",
      maxVal : 200,
      container : container
    });

    app.lag = new SpeedOMeter({
      name : "LAG",
      maxVal : 200,
      container : container
    });

    app.mps = new SpeedOMeter({
      name : "MOVES",
      maxVal : 20,
      container : container
    });

    function setStatus(s) {
      window.document.body.className = s;
    }

    lichess.socket = new $.websocket(lichess.socketUrl + "/monitor/socket", 0, $.extend(true, lichess.socketDefaults, {
      events: {
        monitor: function(msg) {
          var ds = msg.split(";");
          app.lastCall = (new Date()).getTime();
          for(var i in ds) {
            var d = ds[i].split(":");
            if (d.length == 2) {
              if (typeof app[d[1]] != "undefined") {
                app[d[1]].update(d[0]);
              }
            }
          }
        },
        n: function() {
          app["lag"].update(lichess.socket.currentLag);
        }
      },
      options: {
        name: "monitor"
      }
    }));

    setInterval(function () {
      if ((new Date()).getTime() - app.lastCall > 3000) {
        setStatus("down");
      } else if (app.lastCall) {
        setStatus("up");
      }
    },1100);
  }

  window.document.addEventListener("DOMContentLoaded", init, false);

})(window.App);
