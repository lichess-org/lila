var m = require('mithril');

var board = require('./board');
var progress = require('./shared').progress;
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
    m('a.back', {
      'data-icon': 'L',
      onclick: function() {
        ctrl.vm.inspecting = null;
      }
    }),
    resultBar(o),
    m('h2', [
      m('strong', family),
      m('em', d.moves[family]),
      progress(o.ratingDiff)
    ]),
    m('div.content', [
      board(ctrl, family),
      m('div.right', [
        m('table', [
          m('tr', [
            m('th', 'Played in'),
            m('tr', [
              m('strong', o.nbGames),
              ' games (',
              m('strong', Math.round(o.nbGames * 100 / ctrl.data.colorResults.nbGames)),
              '%)'
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
            m('tr', m('time.moment-from-now', {
              datetime: o.lastPlayed
            }))
          ]),
          m('tr', m('th[colspan=2]', 'Average centipawn loss')), [
            ['opening', 'Opening'],
            ['middle', 'Middlegame'],
            ['end', 'Endgame'],
            ['all', 'Overall']
          ].map(function(x) {
            var acpl = o.gameSections[x[0]].acplAvg;
            return m('tr', [
              m('th', x[1]),
              m('td.acpl', [
                m('span', acpl),
                m('div', {
                  class: acpl < 40 ? 'good' : (acpl < 80 ? 'med' : 'bad'),
                  key: x[0],
                  style: {
                    width: Math.min(acpl, 100) + '%'
                  }
                })
              ])
            ]);
          })
        ])
      ])
    ])
  ]);
};
