import Lpv from '@lichess-org/pgn-viewer';
import type { Opts as LpvOpts } from '@lichess-org/pgn-viewer/interfaces';
import type PgnViewer from '@lichess-org/pgn-viewer/pgnViewer';

import { text as xhrText } from 'lib/xhr';

export default async function (
  opts: { el: HTMLElement; url?: string; lpvOpts?: LpvOpts } = { el: document.body },
): Promise<void> {
  const { el, url, lpvOpts } = opts;
  await site.asset.loadCssPath('bits.lpv');
  if (!url) return autostart(el);
  const pgn = await xhrText(url, { headers: { Accept: 'application/x-chess-pgn' } });
  Lpv(el, { ...lpvOpts, lichess: location.origin, pgn });
}

async function autostart(contextEl: HTMLElement = document.body) {
  contextEl.querySelectorAll<HTMLElement>('.lpv--autostart').forEach(el => {
    if (!el.dataset.pgn) return; // already processed
    const pgn = el.dataset['pgn'].replace(/<br>/g, '\n');
    const gamebook = pgn.includes('[ChapterMode "gamebook"]');
    const rawPly = el.dataset['ply'];
    const initialPly =
      rawPly === 'last' ? 'last' : rawPly !== undefined ? parseInt(rawPly, 10) || 0 : undefined;
    const config: Partial<LpvOpts> = {
      pgn,
      orientation: el.dataset['orientation'] as Color | undefined,
      lichess: location.origin,
      initialPly: initialPly ?? (gamebook ? 0 : 'last'),
      ...(gamebook
        ? {
            showPlayers: false,
            showClocks: false,
            showMoves: false,
            showControls: false,
            scrollToMove: false,
          }
        : {}),
    };
    try {
      const lpv = Lpv(el, config);
      if (typeof initialPly === 'number') {
        const rootPly = (lpv.game.mainline[0]?.ply ?? 1) - 1;
        const relativePly = Math.max(0, initialPly - rootPly);
        if (relativePly !== initialPly) lpv.toPath(lpv.game.pathAtMainlinePly(relativePly), false);
      }
      if (gamebook) toGamebook(lpv);
    } catch (e) {
      const url = el.dataset['url'];
      if (url) el.innerHTML = `<a href="${url}">${location.host}${url}</a>`;
      console.warn(`LPV refused to load ${url}: ${e}`);
    }
  });
}

function toGamebook(lpv: PgnViewer) {
  const href = lpv.game.metadata.externalLink;
  $(lpv.div)
    .addClass('lpv--gamebook')
    .append($(`<a href="${href}" target="_blank" class="button lpv__gamebook">Start</a>`));
}
