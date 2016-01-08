var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var button = require('./button');
var pagination = require('../pagination');
var arena = require('./arena');

var oneDayInSeconds = 60 * 60 * 24;

function startingMoment(data) {
  if (!data.secondsToStart) return;
  if (data.secondsToStart > oneDayInSeconds)
    return m('div.tournament_clock.title_tag', [
      m('time.moment-from-now.shy', {
        datetime: data.startsAt
      }, data.startsAt)
    ]);

  return m('div.tournament_clock.title_tag.starting_in', {
    config: util.clock(data.secondsToStart)
  }, [
    m('span.shy', 'Starting in '),
    m('br'),
    m('span.time.text')
  ]);
}

module.exports = {
  main: function(ctrl) {
    var pag = pagination.players(ctrl);
    return [
      startingMoment(ctrl.data),
      util.title(ctrl),
      arena.standing(ctrl, pag, 'created'),
      m('br'),
      m('br'),
      m('blockquote.pull-quote', [
          m('p', ctrl.data.quote.text),
          m('footer', ctrl.data.quote.author)
      ]),
      m('div.content_box_content', {
        config: function(el, isUpdate) {
          if (!isUpdate) $(el).html($('#tournament_faq').show());
        }
      })
    ];
  },
  side: function(ctrl) {
    return null;
  }
};
