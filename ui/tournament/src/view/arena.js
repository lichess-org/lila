var m = require('mithril');
var partial = require('chessground').util.partial;
var tournament = require('../tournament');
var util = require('./util');
var button = require('./button');

function scoreTag(s) {
  return {
    tag: 'span',
    attrs: {
      class: s[1] || ''
    },
    children: [s[0]]
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
      m('td.total',
        m('strong',
          player.sheet.fire ? {
            class: 'is-gold text',
            'data-icon': 'Q'
          } : {}, player.sheet.total))
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

var legend = m('th.legend', [
  m('span.streakstarter', 'Streak starter'),
  m('span.double', 'Double points')
]);

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
          legend,
          m('th', button.joinWithdraw(ctrl))
        ])),
      m('tbody', ctrl.data.players.map(partial(playerTrs, ctrl, maxScore)))
    ];
  }
};
