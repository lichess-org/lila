import { hl, onInsert } from 'lib/snabbdom';
import type LobbyController from '../ctrl';
import type { NowPlaying } from '../interfaces';
import { initMiniBoard } from 'lib/view/miniBoard';
import { timeago } from 'lib/i18n';

function timer(pov: NowPlaying) {
  const date = Date.now() + pov.secondsLeft! * 1000;
  return hl('time.timeago', { hook: onInsert(el => el.setAttribute('datetime', '' + date)) }, timeago(date));
}

export default function (ctrl: LobbyController) {
  return hl(
    'div.now-playing',
    ctrl.data.nowPlaying.map(pov =>
      hl('a.' + pov.variant.key, { key: `${pov.gameId}${pov.lastMove}`, attrs: { href: '/' + pov.fullId } }, [
        hl('span.mini-board.cg-wrap.is2d', {
          attrs: { 'data-state': `${pov.fen},${pov.orientation || pov.color},${pov.lastMove}` },
          hook: { insert: vnode => initMiniBoard(vnode.elm as HTMLElement) },
        }),
        hl('span.meta', [
          !!pov.opponent.ai
            ? i18n.site.aiNameLevelAiLevel('Stockfish', pov.opponent.ai)
            : pov.opponent.username,
          hl(
            'span.indicator',
            pov.isMyTurn
              ? !!pov.secondsLeft && pov.hasMoved
                ? timer(pov)
                : [i18n.site.yourTurn]
              : hl('span', '\xa0'),
          ),
        ]),
      ]),
    ),
  );
}
