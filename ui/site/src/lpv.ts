import Lpv from 'lichess-pgn-viewer';
import { loadCssPath } from './component/assets';

export default function autostart() {
  $('.lpv--autostart').each(function (this: HTMLElement) {
    loadCssPath('lpv').then(() => {
      Lpv(this, {
        pgn: this.dataset['pgn']!,
        showMoves: !!this.dataset['showmoves'],
        scrollToMove: true,
      });
    });
  });
}
