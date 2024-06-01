import Lpv from 'lichess-pgn-viewer';
import PgnViewer from 'lichess-pgn-viewer/pgnViewer';
import { Opts as LpvOpts } from 'lichess-pgn-viewer/interfaces';
import { text as xhrText } from 'common/xhr';

export default async function (opts?: { el: HTMLElement; url: string; lpvOpts: LpvOpts }) {
  return opts ? loadPgnAndStart(opts.el, opts.url, opts.lpvOpts) : autostart();
}

function autostart() {
  $('.lpv--autostart').each(function (this: HTMLElement) {
    const pgn = this.dataset['pgn']!.replace(/<br>/g, '\n');
    const gamebook = pgn.includes('[ChapterMode "gamebook"]');
    site.asset.loadCssPath('bits.lpv').then(() => {
      const config: Partial<LpvOpts> = {
        pgn,
        orientation: this.dataset['orientation'] as Color | undefined,
        lichess: location.origin,
        initialPly: (this.dataset['ply'] as number | 'last') ?? (gamebook ? 0 : 'last'),
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
      const lpv = Lpv(this, config);
      if (gamebook) toGamebook(lpv);
    });
  });
  return Promise.resolve(undefined);
}

async function loadPgnAndStart(el: HTMLElement, url: string, opts: LpvOpts) {
  await site.asset.loadCssPath('bits.lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  return Lpv(el, { ...opts, pgn });
}

function toGamebook(lpv: PgnViewer) {
  const href = lpv.game.metadata.externalLink;
  $(lpv.div)
    .addClass('lpv--gamebook')
    .append($(`<a href="${href}" target="_blank" rel="noopener" class="button lpv__gamebook">Start</a>`));
}
