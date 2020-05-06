import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import { spinner, bind, userName, dataIcon, player as renderPlayer, numberRow  } from './util';
import { Player, PlayerExt, Pairing } from '../interfaces';
import SwissCtrl from '../ctrl';

export default function(ctrl: SwissCtrl): VNode {
  if (!ctrl.playerInfoId) return;
  const data = ctrl.data.playerInfo;
  const noarg = ctrl.trans.noarg;
  const tag = 'div.swiss__player-info.swiss__table';
  if (data?.user.id !== ctrl.playerInfoId) return h(tag, [
    h('div.stats', [
      h('h2', ctrl.playerInfoId),
      spinner()
    ])
  ]);
  const games = data.pairings.filter(p => p).length;
  const wins = data.pairings.filter(p => p?.w).length;
  const avgOp: number | undefined = games ?
    Math.round(data.pairings.reduce((r, p) => r + (p ? p.rating : 0), 0) / games) :
    undefined;
  return h(tag, {
    hook: {
      insert: setup,
      postpatch(_, vnode) { setup(vnode) }
    }
  }, [
    h('a.close', {
      attrs: dataIcon('L'),
      hook: bind('click', () => ctrl.showPlayerInfo(data), ctrl.redraw)
    }),
    h('div.stats', [
      h('h2', [
        h('span.rank', data.rank + '. '),
        renderPlayer(data, true, false)
      ]),
      h('table', [
          numberRow('Points', data.points, 'raw'),
          numberRow('Tie break', data.tieBreak, 'raw'),
          ...(games ? [
            data.performance ? numberRow(
              noarg('performance'),
              data.performance + (games < 3 ? '?' : ''),
              'raw') : null,
            numberRow(noarg('winRate'), [wins, games], 'percent'),
            numberRow(noarg('averageOpponent'), avgOp, 'raw')
          ] : [])
      ])
    ]),
    h('div', [
      h('table.pairings.sublist', {
        hook: bind('click', e => {
          const href = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-href');
          if (href) window.open(href, '_blank');
        })
      }, data.pairings.map((p, i) => {
        const round = ctrl.data.round - i;
        if (!p) return h('tr', [
          h('th', '' + round),
          h('td.bye', {
            attrs: { colspan: 3},
          }, 'Bye'),
          h('td', '½')
        ]);
        const res = result(p);
        return h('tr.glpt.' + (res === '1' ? ' win' : (res === '0' ? ' loss' : '')), {
          key: p.g,
          attrs: { 'data-href': '/' + p.g + (p.c ? '' : '/black') },
          hook: {
            destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement)
          }
        }, [
          h('th', '' + round),
          h('td', userName(p.user)),
          h('td', '' + p.rating),
          h('td.is.color-icon.' + (p.c ? 'white' : 'black')),
          h('td', res)
        ]);
      }))
    ])
  ]);
};

function result(p: Pairing): string {
  switch (p.w) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return p.o ? '*' : '½';
  }
}

function setup(vnode: VNode) {
  const el = vnode.elm as HTMLElement, p = window.lichess.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}
