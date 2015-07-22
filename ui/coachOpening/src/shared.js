var m = require('mithril');

function decimals(nb) {
  return Number(nb).toFixed(2);
}

var strings = {
  acpl: 'Average centipawn loss',
  ratingDiff: 'Rating points won in this opening, in average',
  result: 'Wins, draws and losses'
};

module.exports = {
  strings: strings,
  progress: function(r) {
    var perf;
    var dec = decimals(r > 0 ? r : -r);
    if (r === 0) perf = m('span', ' =');
    else if (r > 0) perf = m('span.positive[data-icon=N]', dec);
    else if (r < 0) perf = m('span.negative[data-icon=M]', dec);
    return m('span.rating.progress.hint--top', {
      'data-hint': strings.ratingDiff
    }, perf);
  },
  resultBar: function(o) {
    return m('div.result-bar', [
      ['nbWin', 'win'],
      ['nbDraw', 'draw'],
      ['nbLoss', 'loss']
    ].map(function(x) {
      var k = x[0];
      var name = x[1];
      var percent = (o[k] * 100 / o.nbGames);
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
  momentFromNow: function(date) {
    return m('time.moment-from-now', {
      config: function(el) {
        $('body').trigger('lichess.content_loaded');
      },
      datetime: date
    });
  },
  chart: {
    makeFont: function(size) {
      return size + "px 'Noto Sans', 'Lucida Grande', 'Lucida Sans Unicode', Verdana, Arial, Helvetica, sans-serif";
    }
  }
};
