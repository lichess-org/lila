var m = require('mithril');

var board = require('./board');
var moves = require('./moves');
var progress = require('./shared').progress;
var momentFromNow = require('./shared').momentFromNow;
var resultBar = require('./shared').resultBar;

function bestWin(w) {
  if (!w.user) return;
  return m('a', {
    href: '/' + w.id
  }, [
    w.user.title ? (w.user.title + ' ') : '',
    w.user.name,
    ' (',
    m('strong', w.rating),
    ')'
  ]);
}

module.exports = function(ctrl, inspecting) {
  var d = ctrl.data;
  var family = inspecting.family;
  var o = d.openings.map[family];
  return m('div.top.inspect', [
    m('a.to.back', {
      'data-icon': 'L',
      onclick: ctrl.uninspect
    }),
    m('a.to.prev', {
      'data-icon': 'I',
      onclick: function() {
        ctrl.jumpBy(-1);
      }
    }),
    m('a.to.next', {
      'data-icon': 'H',
      onclick: function() {
        ctrl.jumpBy(1);
      }
    }),
    resultBar(o),
    m('h2', [
      m('strong', family),
      m('em', d.moves[family]),
      progress(o.ratingDiff / o.nbGames)
    ]),
    m('div.content', [
      board(ctrl, family),
      m('div.right', [
        moves(ctrl, family),
        m('table', [
          m('tr', [
            m('th', 'Played in'),
            m('tr', [
              m('a', [
                m('strong', o.nbGames),
                ' games (',
                m('strong', Math.round(o.nbGames * 100 / ctrl.data.colorResults.nbGames)),
                '%)'
              ])
            ])
          ]),
          m('tr', [
            m('th', 'Computer analysed in'),
            m('tr', [
              m('a', [
                m('strong', o.nbAnalysis),
                ' games (',
                m('strong', Math.round(o.nbAnalysis * 100 / o.nbGames)),
                '%)'
              ])
            ])
          ]),
          m('tr', [
            m('th', 'Average opponent'),
            m('tr', m('strong', o.opponentRatingAvg))
          ]),
          o.bestWin ? m('tr', [
            m('th', 'Best win'),
            m('tr', bestWin(o.bestWin))
          ]) : null,
          m('tr', [
            m('th', 'Last played'),
            m('tr', momentFromNow(o.lastPlayed))
          ])
        ])
      ])
    ])
  ]);
};
