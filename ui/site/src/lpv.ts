import { text as xhrText } from 'common/xhr';
import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';

export default function autostart() {
  $('.lpv--autostart').each(function (this: HTMLElement) {
    loadCssPath('lpv').then(() => {
      Lpv(this, {
        pgn: this.dataset['pgn']!,
      });
    });
  });
}

export const loadPgnAndStart = async (el: HTMLElement, url: string) => {
  await loadCssPath('lpv');
  const pgn = await xhrText(url, {
    headers: {
      Accept: 'application/x-chess-pgn',
    },
  });
  return Lpv(el, { pgn });
};
