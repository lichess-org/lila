import { timeago } from 'lib/i18n';
import { onInsert, initMiniBoard, div, time, a, span } from 'lib/view';

import type LobbyController from '@/ctrl';
import type { NowPlaying } from '@/interfaces';

function timer(pov: NowPlaying) {
  const date = Date.now() + pov.secondsLeft! * 1000;
  return time('.timeago', { hook: onInsert(el => el.setAttribute('datetime', String(date))) }, timeago(date));
}

export default function ({ data }: LobbyController) {
  return div(
    '.now-playing',
    data.nowPlaying.map(pov =>
      a('/' + pov.fullId)(`.${pov.variant.key}`, { key: `${pov.gameId}${pov.lastMove}` }, [
        span('.mini-board.cg-wrap.is2d', {
          'data-state': `${pov.fen},${pov.orientation || pov.color},${pov.lastMove}`,
          hook: onInsert(initMiniBoard),
        }),
        span('.meta', [
          pov.opponent.ai
            ? i18n.site.aiNameLevelAiLevel('Stockfish', pov.opponent.ai)
            : pov.opponent.username,
          span(
            '.indicator',
            pov.isMyTurn
              ? !!pov.secondsLeft && pov.hasMoved
                ? timer(pov)
                : i18n.site.yourTurn
              : span('\xa0'),
          ),
        ]),
      ]),
    ),
  );
}
