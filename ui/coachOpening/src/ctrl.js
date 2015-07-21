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

  this.list = Object.keys(this.data.openings.map).map(function(name) {
    var o = this.data.openings.map[name];
    return copy(o, {
      name: name,
      result: o.nbWin / o.nbLoss,
      acpl: o.gameSections.all.acplAvg
    });
  }.bind(this));

  this.list.sort(function(a, b) {
    return a.nbGames < b.nbGames ? 1 : a.nbGames > b.nbGames ? -1 : 0;
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
      family: 'Sicilian Defence',
      chessground: null
    } */
  };

  this.isInspecting = function(family) {
    return this.vm.inspecting && this.vm.inspecting.family === family;
  }.bind(this);

  this.inspect = function(family) {
    if (this.isInspecting(family)) return;
    var steps = this.data.steps[family];
    var last = steps[steps.length - 1];
    var config = {
      fen: last.fen,
      orientation: this.data.color,
      viewOnly: true,
      minimalDom: true,
      check: last.check,
      lastMove: [last.uci.substr(0, 2), last.uci.substr(2, 2)],
      coordinates: false
    };
    if (this.vm.inspecting) {
      this.vm.inspecting.family = family;
      this.vm.inspecting.chessground.set(config);
    } else
      this.vm.inspecting = {
        family: family,
        chessground: new chessground.controller(config)
      };
  }.bind(this);
  this.inspect('King Pawn Game');

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
