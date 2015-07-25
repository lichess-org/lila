var m = require('mithril');

var chessground = require('chessground');
var slider = require('./slider');
var throttle = require('./shared').throttle;

function copy(obj, newValues) {
  var k, c = {};
  for (k in obj) {
    c[k] = obj[k];
  }
  for (k in newValues) {
    c[k] = newValues[k];
  }
  return c;
}

module.exports = function(opts) {

  this.user = opts.user;
  this.color = opts.color;
  this.nbPeriods = opts.nbPeriods;

  this.vm = {
    preloading: !!this.nbPeriods,
    loading: true,
    range: [0, this.nbPeriods - 1],
    sort: {
      prop: 'nbGames',
      order: -1
    },
    chart: null,
    hover: null,
    inspecting: null,
    /* {
      eco: 'D00',
      chessground: null
    } */
  };

  var requestData = throttle(1000, false, function() {
    m.request({
      url: '/coach/opening/' + this.user.id + '/' + this.color + '.json',
      data: {
        range: this.vm.range.join('-')
      }
    }).then(function(data) {

      this.data = data;

      this.list = Object.keys(this.data.openings).map(function(eco) {
        var o = this.data.openings[eco];
        var r = o.results;
        return copy(o, {
          result: r.nbWin / r.nbLoss,
          acpl: r.gameSections.all.acpl.avg,
          ratingDiffAvg: r.nbGames > 0 ? r.ratingDiff / r.nbGames : 0,
          // just for sorting
          name: o.opening.name,
          nbGames: r.nbGames,
          lastPlayed: r.lastPlayed
        });
      }.bind(this));

      this.sortList();

      if (location.hash) this.inspect(location.hash.replace(/#/, '').replace(/_/g, ' '));

      this.vm.preloading = false;
      this.vm.loading = false;
      m.redraw();
    }.bind(this));
  }.bind(this));
  if (this.nbPeriods) setTimeout(requestData, 200);

  this.selectPeriodRange = function(from, to) {
    this.vm.range = [from, to];
    this.vm.loading = true;
    m.redraw();
    requestData();
  }.bind(this);

  this.sortList = function() {
    var s = this.vm.sort;
    this.list.sort(function(a, b) {
      return a[s.prop] > b[s.prop] ? s.order : a[s.prop] < b[s.prop] ? -s.order : 0;
    });
  }.bind(this);

  this.setSort = function(prop) {
    if (this.vm.sort.prop === prop) this.vm.sort.order = -this.vm.sort.order;
    else this.vm.sort = {
      prop: prop,
      order: 1
    };
    this.sortList();
  }.bind(this);

  this.jumpBy = function(delta) {
    if (!this.vm.inspecting) return;
    var ecos = this.list.map(function(o) {
      return o.opening.eco;
    });
    var i = ecos.indexOf(this.vm.inspecting.eco);
    var i2 = (i + delta) % ecos.length;
    if (i2 < 0) i2 = ecos.length - 1;
    this.inspect(ecos[i2]);
  }.bind(this);

  this.isInspecting = function(eco) {
    return this.vm.inspecting && this.vm.inspecting.eco === eco;
  }.bind(this);

  this.inspect = function(eco) {
    if (!this.data.openings[eco]) return;
    if (this.isInspecting(eco)) return;
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#' + eco);
    var opening = this.data.openings[eco].opening;
    var config = {
      fen: opening.fen,
      orientation: this.color,
      viewOnly: true,
      minimalDom: true,
      lastMove: opening.lastMoveUci ? [opening.lastMoveUci.substr(0, 2), opening.lastMoveUci.substr(2, 2)] : null,
      coordinates: false
    };
    if (this.vm.inspecting) {
      this.vm.inspecting.eco = eco;
      this.vm.inspecting.chessground.set(config);
    } else
      this.vm.inspecting = {
        eco: eco,
        chessground: new chessground.controller(config)
      };
  }.bind(this);

  this.uninspect = function() {
    this.vm.inspecting = null;
    if (window.history.replaceState)
      window.history.replaceState(null, null, '#');
  }.bind(this);

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
