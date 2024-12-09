import { h } from 'snabbdom';
import type LobbyController from '../ctrl';
import type { NowPlaying } from '../interfaces';
import { initMiniBoard } from 'common/miniBoard';
import { timeago } from 'common/i18n';

function timer(pov: NowPlaying) {
  const date = Date.now() + pov.secondsLeft! * 1000;
  return h(
    'time.timeago',
    {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).setAttribute('datetime', '' + date);
        },
      },
    },
    timeago(date),
  );
}

export default function (ctrl: LobbyController) {
  return h(
    'div.now-playing',
    ctrl.data.nowPlaying.map(pov =>
      h('a.' + pov.variant.key, { key: `${pov.gameId}${pov.lastMove}`, attrs: { href: '/' + pov.fullId } }, [
        h('span.mini-board.cg-wrap.is2d', {
          attrs: { 'data-state': `${pov.fen},${pov.orientation || pov.color},${pov.lastMove}` },
          hook: { insert: vnode => initMiniBoard(vnode.elm as HTMLElement) },
        }),
        h('span.meta', [
          pov.opponent.ai
            ? i18n.site.aiNameLevelAiLevel('Stockfish', pov.opponent.ai)
            : pov.opponent.username,
          h(
            'span.indicator',
            pov.isMyTurn
              ? pov.secondsLeft && pov.hasMoved
                ? timer(pov)
                : [i18n.site.yourTurn]
              : h('span', '\xa0'),
          ), // &nbsp;
        ]),
      ]),
    ),
  );
}
