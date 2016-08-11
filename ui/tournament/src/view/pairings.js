var m = require('mithril');
var partial = require('chessground').util.partial;
var opposite = require('chessground').util.opposite;
var util = require('./util');
var status = require('game').status;

function user(p, it) {
  return {
    tag: p.s === 0 ? 'playing' : (
      p.s === 1 ? 'draw' : (
        (p.s === 2) === (it === 0) ? 'win' : 'loss'
      )
    ),
    children: [p.u[it]]
  };
}

function featuredPlayer(f, orientation) {
  var p = f[orientation === 'top' ? opposite(f.color) : f.color];
  return m('div.vstext.' + orientation, [
    p.berserk ? m('i[data-icon=`][title=Berserk]') : null,
    m('strong', '#' + p.rank),
    util.player(p)
  ])
}

function featured(f) {
  return m('div.featured', [
    featuredPlayer(f, 'top'),
    util.miniBoard(f),
    featuredPlayer(f, 'bottom')
  ]);
}

function nextTournament(ctrl) {
  var t = ctrl.data.next;
  if (t) return [
    m('a.next', {
      href: '/tournament/' + t.id
    }, [
      m('i', {
        'data-icon': t.perf.icon
      }),
      m('span.content', [
        m('span', 'Next ' + t.perf.name + ' tournament:'),
        m('span.name', t.name),
        m('span.more', [
          ctrl.trans('nbConnectedPlayers', t.nbPlayers),
          ' • ',
          t.finishesAt ? [
            'finishes ',
            m('time.moment-from-now', {
              datetime: t.finishesAt
            }, t.finishesAt)
          ] : m('time.moment-from-now', {
            datetime: t.startsAt
          }, t.startsAt)
        ])
      ])
    ]),
    m('a.others[href=/tournament]', 'View more tournaments')
  ];
}

module.exports = function(ctrl) {
  var pairing = function(p) {
    return {
      tag: 'a',
      attrs: {
        key: p.id,
        href: '/' + p.id
      },
      children: [
        user(p, 0),
        'vs',
        user(p, 1)
      ]
    };
  };
  return [
    ctrl.data.featured ? featured(ctrl.data.featured) : nextTournament(ctrl),
    m('div.box.all_pairings.scroll-shadow-soft', {
      onclick: function() {
        return !ctrl.vm.disableClicks;
      }
    }, ctrl.data.pairings.map(pairing))
  ];
};
