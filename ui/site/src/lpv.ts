import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';
import { Opts } from 'lichess-pgn-viewer/interfaces';
import { text as xhrText } from 'common/xhr';

export function autostart() {
  $('.lpv--autostart').each(function (this: HTMLElement) {
    loadCssPath('lpv').then(() => {
      Lpv(this, {
        pgn: this.dataset['pgn']!.replace(/<br>/g, '\n'),
        orientation: this.dataset['orientation'] as Color | undefined,
        lichess: location.origin,
      });
    });
  });
}

export const loadPgnAndStart = async (el: HTMLElement, url: string, opts: Opts) => {
  await loadCssPath('lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  return Lpv(el, { ...opts, pgn });
};
