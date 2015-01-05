var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');

var legend = m('th.legend', [
  m('span.streakstarter', 'Streak starter'),
  m('span.double', 'Double points')
]);

function scoreTag(s) {
  return {
    tag: 'span',
    children: [s]
  };
}

function playerTrs(ctrl, maxScore, player) {
  return [{
    tag: 'tr',
    attrs: {
      key: player.id,
      class: ctrl.userId === player.id ? '.me' : ''
    },
    children: [
      m('td.name', [
        player.withdraw ? m('span', {
          'data-icon': 'b',
          'title': ctrl.trans('withdraw')
        }) : (
          (ctrl.data.isFinished && player.rank === 1) ? m('span', {
            'data-icon': 'g',
            'title': ctrl.trans('winner')
          }) : m('span.rank', player.rank)),
        util.player(player)
      ]),
      m('td.sheet', player.sheet.scores.map(scoreTag)),
      m('td.total', [
        m('strong', player.sheet.total),
        m('span', {
          class: 'neustadtl',
          title: 'Tie-breaker "Neustadtl" score'
        }, player.sheet.neustadtl)
      ])
    ]
  }, {
    tag: 'tr',
    children: [
      m('td', {
        class: 'around-bar',
        colspan: 3
      }, m('div', {
        class: 'bar',
        style: {
          width: Math.ceil(player.sheet.total * 100 / maxScore) + '%'
        }
      }))
    ]
  }];
}

module.exports = {
  standing: function(ctrl) {
    var maxScore = Math.max.apply(Math, ctrl.data.players.map(function(p) {
      return p.sheet.total;
    }));
    return [
      m('thead',
        m('tr', [
          m('th.large', [
            ctrl.trans('standing') + ' (' + ctrl.data.players.length + ')'
          ]),
          m('th'),
          m('th', button.joinWithdraw(ctrl))
        ])),
      m('tbody', ctrl.data.players.map(partial(playerTrs, ctrl, maxScore)))
    ];
  }
};
