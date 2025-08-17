import type { VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { bind, dataIcon, hl } from 'lib/snabbdom';
import { player as renderPlayer, numberRow } from './util';
import type { Pairing } from '../interfaces';
import { isOutcome } from '../util';
import type SwissCtrl from '../ctrl';
import { fullName } from 'lib/view/userLink';

export default function (ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.playerInfoId) return;
  const data = ctrl.data.playerInfo;
  const tag = 'div.swiss__player-info.swiss__table';
  if (data?.user.id !== ctrl.playerInfoId)
    return hl(tag, [hl('div.stats', [hl('h2', ctrl.playerInfoId), spinner()])]);
  const games = data.sheet.filter(p => !isOutcome(p) && p.g).length;
  const wins = data.sheet.filter(p => !isOutcome(p) && p.w).length;
  const avgOp: number | undefined = games
    ? Math.round(data.sheet.reduce((r, p) => r + (!isOutcome(p) ? p.rating : 1), 0) / games)
    : undefined;
  return hl(tag, { hook: { insert: setup, postpatch: (_, vnode) => setup(vnode) } }, [
    hl('a.close', {
      attrs: dataIcon(licon.X),
      hook: bind('click', () => ctrl.showPlayerInfo(data), ctrl.redraw),
    }),
    hl('div.stats', [
      hl('h2', [hl('span.rank', data.rank + '. '), renderPlayer(data, true, false)]),
      hl('table', [
        numberRow(i18n.site.points, data.points, 'raw'),
        numberRow(i18n.swiss.tieBreak, data.tieBreak, 'raw'),
        games !== 0 && [
          !!data.performance &&
            ctrl.opts.showRatings &&
            numberRow(i18n.site.performance, data.performance + (games < 3 ? '?' : ''), 'raw'),
          numberRow(i18n.site.winRate, [wins, games], 'percent'),
          ctrl.opts.showRatings && numberRow(i18n.site.averageOpponent, avgOp, 'raw'),
        ],
      ]),
    ]),
    hl('div', [
      hl(
        'table.pairings.sublist',
        {
          hook: bind('click', e => {
            const href = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-href');
            if (href) window.open(href, '_blank');
          }),
        },
        data.sheet.map((p, i) => {
          const round = ctrl.data.round - i;
          if (isOutcome(p))
            return hl('tr.' + p, { key: round }, [
              hl('th', '' + round),
              hl('td.outcome', { attrs: { colspan: 3 } }, p),
              hl('td', p === 'absent' ? '-' : p === 'bye' ? '1' : '½'),
            ]);
          const res = result(p);
          return hl(
            'tr.glpt.' + (res === '1' ? '.win' : res === '0' ? '.loss' : ''),
            {
              key: round,
              attrs: { 'data-href': '/' + p.g + (p.c ? '' : '/black') },
              hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
            },
            [
              hl('th', '' + round),
              hl('td', fullName(p.user)),
              ctrl.opts.showRatings && hl('td', '' + p.rating),
              hl('td.is.color-icon.' + (p.c ? 'white' : 'black')),
              hl('td.result', res),
            ],
          );
        }),
      ),
    ]),
  ]);
}

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
  const el = vnode.elm as HTMLElement,
    p = site.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}
