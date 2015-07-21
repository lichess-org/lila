var m = require('mithril');

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
      result: o.nbWin + o.nbDraw / 2 - o.nbLoss,
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
    hover: null
  };

  this.trans = function(key) {
    var str = env.i18n[key] || key;
    Array.prototype.slice.call(arguments, 1).forEach(function(arg) {
      str = str.replace('%s', arg);
    });
    return str;
  };
};
