import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';
import { Opts as LpvOpts } from 'lichess-pgn-viewer/interfaces';
import { text as xhrText } from 'common/xhr';

export default async function (opts?: { el: HTMLElement; url: string; lpvOpts: LpvOpts }) {
  return opts ? loadPgnAndStart(opts.el, opts.url, opts.lpvOpts) : autostart();
}

function autostart() {
  $('.lpv--autostart').each(function (this: HTMLElement) {
    loadCssPath('lpv').then(() => {
      Lpv(this, {
        pgn: this.dataset['pgn']!.replace(/<br>/g, '\n'),
        orientation: this.dataset['orientation'] as Color | undefined,
        lichess: location.origin,
        initialPly: this.dataset['ply'] as number | 'last',
      });
    });
  });
  return Promise.resolve(undefined);
}

async function loadPgnAndStart(el: HTMLElement, url: string, opts: LpvOpts) {
  await loadCssPath('lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  return Lpv(el, { ...opts, pgn });
}
