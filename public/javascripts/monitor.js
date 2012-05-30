(function (app) {

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

      app.rps = new SpeedOMeter({
        name : "RPS",
      maxVal : 50,
      threshold: 0.9,
      container : container
      });

    app.memory = new SpeedOMeter({
      name : "MEMORY",
      maxVal : app.totalMemory,
      threshold: 0.9,
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
      maxVal : 200,
      threshold: 0.5,
      container : container
    });

    app.load = new SpeedOMeter({
      name : "LOAD",
      maxVal : 1,
      threshold: 0.3,
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
      maxVal : 3000,
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
      maxVal : 2,
      container : container
    });

    app.ai = new SpeedOMeter({
      name : "AI PING",
      maxVal : 1000,
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
