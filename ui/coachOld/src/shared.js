var m = require('mithril');

var strings = {
  acpl: 'Average centipawn loss per move',
  ratingDiff: 'Average rating points won',
  result: 'Wins, draws and losses'
};

function decimals(nb) {
  return Number(nb).toFixed(2);
}

module.exports = {
  strings: strings,
  userLink: function(username) {
    return m('a', {
      href: '/@/' + username
    }, username);
  },
  momentFromNow: function(date) {
    return m('time.moment-from-now', {
      datetime: date
    });
  },
  momentFormat: function(date, format) {
    var parsed = moment(date);
    var textContent = (format || 'calendar') === 'calendar' ? parsed.calendar() : parsed.format(format);
    return m('time', {
      datetime: date
    }, format == 'calendar' ? parsed.calendar() : parsed.format(format));
  },
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
};
