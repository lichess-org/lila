import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { type VNode, bind, dataIcon, looseH as h } from 'common/snabbdom';
import { numberRow, player as renderPlayer } from './util';
import { fullName } from 'common/userLink';
import { teamName } from './battle';
import { ids } from 'game/status';
import type TournamentController from '../ctrl';
import type { Player } from '../interfaces';

function result(win: boolean, stat: number): string {
  switch (win) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return stat >= ids.mate ? '½' : '*';
  }
}

const playerTitle = (player: Player) =>
  h('h2', [player.rank ? h('span.rank', `${player.rank}. `) : '', renderPlayer(player, true, false, false)]);

function setup(vnode: VNode) {
  const el = vnode.elm as HTMLElement,
    p = site.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}

export default function (ctrl: TournamentController): VNode {
  const data = ctrl.playerInfo.data;
  const tag = 'div.tour__player-info.tour__actor-info';
  if (!data || data.player.id !== ctrl.playerInfo.id)
    return h(tag, [h('div.stats', [playerTitle(ctrl.playerInfo.player!), spinner()])]);
  const nb = data.player.nb,
    pairingsLen = data.pairings.length,
    avgOp = pairingsLen
      ? Math.round(data.pairings.reduce((a, b) => a + b.op.rating, 0) / pairingsLen)
      : undefined;
  return h(tag, { hook: { insert: setup, postpatch: (_, vnode) => setup(vnode) } }, [
    h('a.close', {
      attrs: dataIcon(licon.X),
      hook: bind('click', () => ctrl.showPlayerInfo(data.player), ctrl.redraw),
    }),
    h('div.stats', [
      playerTitle(data.player),
      data.player.team &&
        h('team', { hook: bind('click', () => ctrl.showTeamInfo(data.player.team!), ctrl.redraw) }, [
          teamName(ctrl.data.teamBattle!, data.player.team),
        ]),
      h('table', [
        ctrl.opts.showRatings &&
          data.player.performance &&
          numberRow(i18n.site.performance, data.player.performance + (nb.game < 3 ? '?' : ''), 'raw'),
        numberRow(i18n.site.gamesPlayed, nb.game),
        ...(nb.game
          ? [
              numberRow(i18n.site.winRate, [nb.win, nb.game], 'percent'),
              numberRow(i18n.site.berserkRate, [nb.berserk, nb.game], 'percent'),
              ctrl.opts.showRatings && numberRow(i18n.site.averageOpponent, avgOp, 'raw'),
            ]
          : []),
      ]),
    ]),
    h('div', [
      h(
        'table.pairings.sublist',
        {
          hook: bind('click', e => {
            const href = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute('data-href');
            if (href) window.open(href, '_blank', 'noopener');
          }),
        },
        data.pairings.map(function (p, i) {
          const res = result(p.win, p.status);
          return h(
            'tr.glpt.' + (res === '1' ? ' win' : res === '0' ? ' loss' : ''),
            {
              key: p.id,
              attrs: { 'data-href': '/' + p.id + '/' + p.color },
              hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
            },
            [
              h('th', '' + (Math.max(nb.game, pairingsLen) - i)),
              h('td', fullName(p.op)),
              ctrl.opts.showRatings ? h('td', `${p.op.rating}`) : null,
              berserkTd(!!p.op.berserk),
              h('td.is.color-icon.' + p.color),
              h('td.result', res),
              berserkTd(p.berserk),
            ],
          );
        }),
      ),
    ]),
  ]);
}

const berserkTd = (b: boolean) =>
  b ? h('td.berserk', { attrs: { 'data-icon': licon.Berserk, title: 'Berserk' } }) : h('td.berserk');
