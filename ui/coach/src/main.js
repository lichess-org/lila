var m = require('mithril');

var shared = require('./shared');

module.exports = {
  throttle: require('./throttle'),
  slider: require('./slider'),
  shared: shared,
  bestWin: function(w, color) {
    if (!w.user) return;
    return m('a', {
      href: '/' + w.id + '/' + color
    }, [
      w.user.title ? (w.user.title + ' ') : '',
      w.user.name,
      ' (',
      m('strong', w.rating),
      ')'
    ]);
  },
  resultBar: function(r) {
    return m('div.result-bar', [
      ['nbWin', 'win'],
      ['nbDraw', 'draw'],
      ['nbLoss', 'loss']
    ].map(function(x) {
      var k = x[0];
      var name = x[1];
      var percent = (r[k] * 100 / r.nbGames);
      return m('div', {
        key: k,
        class: k,
        style: {
          width: percent + '%'
        }
      }, [
        m('strong', Math.round(percent)),
        '% ' + name
      ]);
    }));
  },
};
