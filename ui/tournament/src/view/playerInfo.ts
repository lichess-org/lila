import * as licon from 'lib/licon';
import { spinnerVdom as spinner } from 'lib/view';
import { type VNode, bind, dataIcon, hl } from 'lib/view';
import { player as renderPlayer } from './util';
import { fullName } from 'lib/view/userLink';
import { numberRow } from 'lib/view/util';
import { teamName } from './battle';
import { status } from 'lib/game';
import type TournamentController from '../ctrl';
import type { Player } from '../interfaces';

const playerTitle = (player: Player) =>
  hl('h2', [
    player.rank ? hl('span.rank', `${player.rank}. `) : '',
    renderPlayer(player, true, false, false),
  ]);

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
    return hl(tag, [hl('div.stats', [playerTitle(ctrl.playerInfo.player!), spinner()])]);
  const nb = data.player.nb,
    pairingsLen = data.pairings.length,
    avgOp = pairingsLen
      ? Math.round(data.pairings.reduce((a, b) => a + b.op.rating, 0) / pairingsLen)
      : undefined;
  return hl(tag, { hook: { insert: setup, postpatch: (_, vnode) => setup(vnode) } }, [
    hl('a.close', {
      attrs: dataIcon(licon.X),
      hook: bind('click', () => ctrl.showPlayerInfo(data.player), ctrl.redraw),
    }),
    hl('div.stats', [
      playerTitle(data.player),
      data.player.team &&
        hl('team', { hook: bind('click', () => ctrl.showTeamInfo(data.player.team!), ctrl.redraw) }, [
          teamName(ctrl.data.teamBattle!, data.player.team),
        ]),
      hl('table', [
        ctrl.opts.showRatings &&
          data.player.performance &&
          numberRow(i18n.site.performance, data.player.performance + (nb.game < 3 ? '?' : ''), 'raw'),
        numberRow(i18n.site.gamesPlayed, nb.game),
        nb.game > 0 && [
          numberRow(i18n.site.winRate, [nb.win, nb.game], 'percent'),
          numberRow(i18n.arena.berserkRate, [nb.berserk, nb.game], 'percent'),
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
            if (href) window.open(href, '_blank', 'noopener');
          }),
        },
        data.pairings.map((p, i) => {
          const score = p.status < status.mate ? '*' : p.score;
          const streak = p.win == null ? p.score === 2 : p.win ? p.score > 3 : false;
          const cls = p.win == null ? '' : streak ? 'streak' : p.win == false ? 'loss' : 'win';
          return hl(
            'tr.glpt.' + cls,
            {
              key: p.id,
              attrs: { 'data-href': '/' + p.id + '/' + p.color },
              hook: { destroy: vnode => $.powerTip.destroy(vnode.elm as HTMLElement) },
            },
            [
              hl('th', '' + (Math.max(nb.game, pairingsLen) - i)),
              hl('td', fullName(p.op)),
              ctrl.opts.showRatings ? hl('td', `${p.op.rating}`) : null,
              berserkTd(!!p.op.berserk),
              hl('td.is.color-icon.' + p.color),
              hl('td.result', score),
              berserkTd(p.berserk),
            ],
          );
        }),
      ),
    ]),
  ]);
}

const berserkTd = (b: boolean) =>
  b ? hl('td.berserk', { attrs: { 'data-icon': licon.Berserk, title: 'Berserk' } }) : hl('td.berserk');
