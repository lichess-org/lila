var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var xhr = require('../xhr');

function header(ctrl) {
  var tour = ctrl.data;
  return [
    m('th.large',
      tour.schedule ? [
        'Starting ',
        util.secondsFromNow(tour.schedule.seconds)
      ] : (
        tour.enoughPlayersToStart ? ctrl.trans('tournamentIsStarting') : ctrl.trans('waitingForNbPlayers', tour.missingPlayers)
      )
    ),
    ctrl.userId ? m('th',
      tournament.containsMe(ctrl) ? [
        (tournament.createdByMe(ctrl) && tour.enoughPlayersToEarlyStart) ?
        m('button.button.right', {
          onclick: partial(xhr.earlyStart, ctrl)
        }, 'Early start') : null,
        m('button.button.right.text', {
          'data-icon': 'b',
          onclick: partial(xhr.withdraw, ctrl)
        }, ctrl.trans('withdraw'))
      ] : m('button.button.right.text', {
        'data-icon': tour.private ? 'a' : 'G',
        onclick: partial(xhr.join, ctrl)
      }, ctrl.trans('join'))
    ) : null
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
      util.title(ctrl),
      m('table.slist.user_list',
        m('thead', m('tr', header(ctrl))),
        m('tbody', ctrl.data.players.map(partial(playerTr, ctrl))))
    ];
  },
  side: function(ctrl) {
    return null;
  }
};
