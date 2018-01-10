import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { opposite } from 'chessground/util';
import { player as renderPlayer, miniBoard, bind } from './util';
import { MaybeVNodes } from '../interfaces';
import TournamentController from '../ctrl';

function user(p, it): VNode {
  return h(
    p.s === 0 ? 'playing' : (p.s === 1 ? 'draw' : ((p.s === 2) === (it === 0) ? 'win' : 'loss')),
    [p.u[it]]);
}

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

function renderPairing(p): VNode {
  return h('a.glpt', {
    key: p.id,
    attrs: { href: '/' + p.id }
  }, [
    user(p, 0),
    '-',
    user(p, 1)
  ]);
}

export default function(ctrl: TournamentController): MaybeVNodes {
  return [
    ...(ctrl.data.featured ? [featured(ctrl.data.featured)] : nextTournament(ctrl)),
    h('div.box.all_pairings.scroll-shadow-soft', {
      hook: bind('click', _ => !ctrl.disableClicks)
    }, ctrl.data.pairings.map(renderPairing))
  ];
};
