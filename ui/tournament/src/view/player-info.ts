import { bind, dataIcon } from 'common/snabbdom';
import spinner from 'common/spinner';
import * as status from 'game/status';
import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type TournamentController from '../ctrl';
import { teamName } from './battle';
import { numberRow, playerName, player as renderPlayer } from './util';

function result(win, stat): string {
  switch (win) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return stat >= status.ids.mate ? '½' : '*';
  }
}

function playerTitle(player) {
  return h('h2', [
    h('span.rank', player.rank ? player.rank + '. ' : ''),
    renderPlayer(player, true, false, false),
  ]);
}

function setup(vnode: VNode) {
  const el = vnode.elm as HTMLElement,
    p = window.lishogi.powertip;
  p.manualUserIn(el);
  p.manualGameIn(el);
}

export default function (ctrl: TournamentController): VNode {
  const data = ctrl.playerInfo.data,
    tag = 'div.tour__player-info.tour__actor-info';
  console.log('PPPP:', !data, data?.player?.id !== ctrl.playerInfo.id);

  if (!data || data.player.id !== ctrl.playerInfo.id)
    return h(tag, [h('div.stats', [playerTitle(ctrl.playerInfo.player), spinner()])]);
  const nb = data.player.nb,
    poa = data.pairings || data.arrangements,
    poaLen = poa.length,
    avgOp = poaLen ? Math.round(poa.reduce((a, b) => a + b.op.rating, 0) / poaLen) : undefined;
  return h(
    tag,
    {
      hook: {
        insert: setup,
        postpatch(_, vnode) {
          setup(vnode);
        },
      },
    },
    [
      h('a.close', {
        attrs: dataIcon('L'),
        hook: bind('click', () => ctrl.showPlayerInfo(data.player), ctrl.redraw),
      }),
      h('div.stats', [
        playerTitle(data.player),
        data.player.team
          ? h(
              'team',
              {
                hook: bind('click', () => ctrl.showTeamInfo(data.player.team), ctrl.redraw),
              },
              [teamName(ctrl.data.teamBattle!, data.player.team)],
            )
          : null,
        h('table', [
          data.player.performance
            ? numberRow(
                i18n('performance'),
                data.player.performance + (nb.game < 3 ? '?' : ''),
                'raw',
              )
            : null,
          numberRow(i18n('gamesPlayed'), nb.game),
          ...(nb.game
            ? [
                numberRow(i18n('winRate'), [nb.win, nb.game], 'percent'),
                numberRow(i18n('berserkRate'), [nb.berserk, nb.game], 'percent'),
                numberRow(i18n('averageOpponent'), avgOp, 'raw'),
              ]
            : []),
        ]),
      ]),
      h('div', [
        h(
          'table.pairings.sublist',
          {
            hook: bind('click', e => {
              const href = ((e.target as HTMLElement).parentNode as HTMLElement).getAttribute(
                'data-href',
              );
              if (href) window.open(href, '_blank');
            }),
          },
          poa.map((p, i) => {
            const res = result(p.win, p.status);
            return h(
              'tr.glpt.' + (res === '1' ? ' win' : res === '0' ? ' loss' : ''),
              {
                key: p.id,
                attrs: { 'data-href': '/' + p.id + '/' + p.color },
                hook: {
                  destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement),
                },
              },
              [
                h('th', '' + (Math.max(nb.game, poaLen) - i)),
                h('td', playerName(p.op)),
                h('td', p.op.rating),
                h('td.is.color-icon.' + p.color),
                h('td', res),
              ],
            );
          }),
        ),
      ]),
    ],
  );
}
