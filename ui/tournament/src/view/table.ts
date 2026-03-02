import { opposite } from '@lichess-org/chessground/util';
import * as licon from 'lib/licon';
import { type VNode, bind, onInsert, hl, initMiniGames } from 'lib/view';
import { player as renderPlayer } from './util';
import type { Duel, DuelPlayer, FeaturedGame, TournamentOpts } from '../interfaces';
import { teamName } from './battle';
import type TournamentController from '../ctrl';

function featuredPlayer(game: FeaturedGame, color: Color, opts: TournamentOpts) {
  const player = game[color];
  return hl('span.mini-game__player', [
    hl('span.mini-game__user', [
      hl('strong', '#' + player.rank),
      renderPlayer(player, true, opts.showRatings, false),
      player.berserk && hl('i.berserk', { attrs: { 'data-icon': licon.Berserk, title: 'Berserk' } }),
    ]),
    game.c
      ? hl(`span.mini-game__clock.mini-game__clock--${color}`, {
          attrs: { 'data-time': game.c[color], 'data-managed': 1 },
        })
      : hl('span.mini-game__result', game.winner ? (game.winner === color ? '1' : '0') : '½'),
  ]);
}

function featured(game: FeaturedGame, opts: TournamentOpts): VNode {
  return hl(
    `div.tour__featured.mini-game.mini-game-${game.id}.mini-game--init.is2d`,
    {
      attrs: { 'data-state': `${game.fen},${game.orientation},${game.lastMove}`, 'data-live': game.id },
      hook: onInsert(site.powertip.manualUserIn),
    },
    [
      featuredPlayer(game, opposite(game.orientation), opts),
      hl('a.cg-wrap', { attrs: { href: `/${game.id}/${game.orientation}` } }),
      featuredPlayer(game, game.orientation, opts),
    ],
  );
}

const duelPlayerMeta = (p: DuelPlayer, ctrl: TournamentController) => [
  hl('em.rank', '#' + p.k),
  p.t && hl('em.utitle', p.t),
  ctrl.opts.showRatings && hl('em.rating', '' + p.r),
];

function renderDuel(ctrl: TournamentController) {
  const battle = ctrl.data.teamBattle,
    duelTeams = ctrl.data.duelTeams;
  return (d: Duel) =>
    hl('a.glpt.force-ltr', { key: d.id, attrs: { href: '/' + d.id } }, [
      battle &&
        duelTeams &&
        hl(
          'line.t',
          d.p.map(p => {
            const teamId = duelTeams[p.n.toLowerCase()];
            return teamId && teamName(battle, teamId);
          }),
        ),
      hl('line.a', [hl('strong', d.p[0].n), hl('span', duelPlayerMeta(d.p[1], ctrl).reverse())]),
      hl('line.b', [hl('span', duelPlayerMeta(d.p[0], ctrl)), hl('strong', d.p[1].n)]),
    ]);
}

const initMiniGame = (node: VNode) => initMiniGames(node.elm as HTMLElement);

export default function (ctrl: TournamentController): VNode {
  return hl('div.tour__table', { hook: { insert: initMiniGame, postpatch: initMiniGame } }, [
    ctrl.data.featured && featured(ctrl.data.featured, ctrl.opts),
    ctrl.data.duels.length > 0 &&
      hl(
        'section.tour__duels',
        { hook: bind('click', _ => !ctrl.disableClicks) },
        [hl('h2', i18n.site.topGames)].concat(ctrl.data.duels.map(renderDuel(ctrl))),
      ),
  ]);
}
