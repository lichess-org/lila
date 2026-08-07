import Lpv from '@lichess-org/pgn-viewer';
import type { Opts as LpvOpts } from '@lichess-org/pgn-viewer/interfaces';
import type PgnViewer from '@lichess-org/pgn-viewer/pgnViewer';

import { text as xhrText } from 'lib/xhr';

export default async function (opts?: { el: HTMLElement; url: string; lpvOpts: LpvOpts }): Promise<void> {
  return opts ? loadPgnAndStart(opts.el, opts.url, opts.lpvOpts) : autostart();
}

async function autostart() {
  await site.asset.loadCssPath('bits.lpv');
  $('.lpv--autostart').each(function (this: HTMLElement) {
    const pgn = this.dataset['pgn']!.replace(/<br>/g, '\n');
    const gamebook = pgn.includes('[ChapterMode "gamebook"]');
    const rawPly = this.dataset['ply'];
    const initialPly =
      rawPly === 'last' ? 'last' : rawPly !== undefined ? parseInt(rawPly, 10) || 0 : undefined;
    const config: Partial<LpvOpts> = {
      pgn,
      orientation: this.dataset['orientation'] as Color | undefined,
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
      const lpv = Lpv(this, config);
      if (typeof initialPly === 'number') {
        const rootPly = (lpv.game.mainline[0]?.ply ?? 1) - 1;
        const relativePly = Math.max(0, initialPly - rootPly);
        if (relativePly !== initialPly) lpv.toPath(lpv.game.pathAtMainlinePly(relativePly), false);
      }
      if (gamebook) toGamebook(lpv);
    } catch (e) {
      const url = this.dataset['url'];
      if (url) this.innerHTML = `<a href="${url}">${location.host}${url}</a>`;
      console.warn(`LPV refused to load ${url}: ${e}`);
    }
  });
}

async function loadPgnAndStart(el: HTMLElement, url: string, opts: LpvOpts) {
  await site.asset.loadCssPath('bits.lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  Lpv(el, {
    ...opts,
    lichess: location.origin,
    pgn,
  });
}

function toGamebook(lpv: PgnViewer) {
  const href = lpv.game.metadata.externalLink;
  $(lpv.div)
    .addClass('lpv--gamebook')
    .append($(`<a href="${href}" target="_blank" class="button lpv__gamebook">Start</a>`));
}
