import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { opposite } from 'chessground/util';
import { player as renderPlayer, miniBoard, bind } from './util';
import { Duel, DuelPlayer, MaybeVNodes } from '../interfaces';
import TournamentController from '../ctrl';

function featuredPlayer(f, orientation) {
  const p = f[orientation === 'top' ? opposite(f.color) : f.color];
  return h('div.vstext.' + orientation, [
    p.berserk ? h('i', {
      attrs: {
        'data-icon': '`',
        title: 'Berserk'
      }
    }) : null,
    h('strong', '#' + p.rank),
    renderPlayer(p, true, true)
  ]);
}

function featured(f): VNode {
  return h('div.featured', [
    featuredPlayer(f, 'top'),
    miniBoard(f),
    featuredPlayer(f, 'bottom')
  ]);
}

function nextTournament(ctrl: TournamentController): MaybeVNodes {
  const t = ctrl.data.next;
  return t ? [
    h('a.next', { attrs: { href: '/tournament/' + t.id } }, [
      h('i', { attrs: { 'data-icon': t.perf.icon } }),
      h('span.content', [
        h('span', ctrl.trans('nextXTournament', t.perf.name)),
        h('span.name', t.name),
        h('span.more', [
          ctrl.trans('nbPlayers', t.nbPlayers),
          ' • ',
          ...(t.finishesAt ? [
            'finishes ',
            h('time.timeago', { attrs: { datetime: t.finishesAt } })
          ] : [
            h('time.timeago', { attrs: { datetime: t.startsAt } })
          ])
        ])
      ])
    ]),
    h('a.others', { attrs: { href: '/tournament' } }, ctrl.trans.noarg('viewMoreTournaments'))
  ] : [];
}

function duelPlayerMeta(p: DuelPlayer) {
  return [
    h('em.rank', '#' + p.k),
    p.t ? h('em.title', p.t) : null,
    h('em.rating', '' + p.r)
  ];
}

function renderDuel(d: Duel): VNode {
  return h('a.glpt', {
    key: d.id,
    attrs: { href: '/' + d.id }
  }, [
    h('line.a', [
      h('strong', d.p[0].n),
      h('span', duelPlayerMeta(d.p[1]).reverse())
    ]),
    h('line.b', [
      h('span', duelPlayerMeta(d.p[0])),
      h('strong', d.p[1].n)
    ])
  ]);
}

export default function(ctrl: TournamentController): MaybeVNodes {
  return [
    ...(ctrl.data.featured ? [featured(ctrl.data.featured)] : nextTournament(ctrl)),
    ctrl.data.duels.length ? h('div.duels', {
      hook: bind('click', _ => !ctrl.disableClicks)
    }, [h('h3', 'Top games')].concat(ctrl.data.duels.map(renderDuel))) : null
  ];
};
