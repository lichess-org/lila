var m = require('mithril');

var chessground = require('chessground');

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

  this.data = opts.data;
  window.openingData = opts.data;

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

  this.list.sort(function(a, b) {
    return a.results.nbGames < b.results.nbGames ? 1 : a.results.nbGames > b.results.nbGames ? -1 : 0;
  });

  this.vm = {
    sort: {
      field: 'nbGames',
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
      orientation: this.data.color,
      viewOnly: true,
      minimalDom: true,
      lastMove: [opening.lastMoveUci.substr(0, 2), opening.lastMoveUci.substr(2, 2)],
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

  if (location.hash) this.inspect(location.hash.replace(/#/, '').replace(/_/g, ' '));

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
