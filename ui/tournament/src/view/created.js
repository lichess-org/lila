var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var button = require('./button');
var xhr = require('../xhr');

function header(ctrl) {
  var tour = ctrl.data;
  return [
    m('th.large', tour.players.length + ' Players'),
    ctrl.userId ? m('th',
      tournament.containsMe(ctrl) ? button.withdraw(ctrl) : button.join(ctrl)
    ) : m('th')
  ];
}

function playerTr(ctrl, player) {
  return m('tr',
    m('td', {
      colspan: 2
    }, util.player(player)));
}

module.exports = {
  main: function(ctrl) {
    return [
      ctrl.data.secondsToStart ? m('div.tournament_clock.title_tag', {
        config: util.clock(ctrl.data.secondsToStart)
      }, [
        m('span.shy', 'Starting in '),
        m('span.time.text')
      ]) : null,
      util.title(ctrl),
      m('table.slist.user_list',
        m('thead', m('tr', header(ctrl))),
        m('tbody', ctrl.data.players.map(partial(playerTr, ctrl)))),
      m('br'),
      m('br'),
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
