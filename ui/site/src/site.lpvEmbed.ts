import Lpv from 'lichess-pgn-viewer';
import type { Opts } from 'lichess-pgn-viewer/interfaces';

interface OptsWithI18n extends Opts {
  i18n: any;
  gamebook?: {
    url: string;
  };
}

export function initModule(opts: Partial<OptsWithI18n>) {
  const elem = document.body.firstChild!.firstChild as HTMLElement;
  const lpv = Lpv(elem, {
    initialPly: parseInt(location.hash.slice(1)) || undefined,
    ...(opts.gamebook
      ? {
          showPlayers: false,
          showMoves: false,
          showClocks: false,
          showControls: false,
          scrollToMove: false,
          drawArrows: false,
          classes: 'lpv--gamebook',
        }
      : {
          showMoves: 'auto',
        }),
    ...opts,
    pgn: elem.innerHTML,
    translate: key => opts.i18n?.[key],
  });
  if (opts.gamebook) {
    const text = lpv.game.initial.comments[0] || 'Start';
    lpv.div?.insertAdjacentHTML(
      'beforeend',
      `<a href="${opts.gamebook.url}" target="_blank" class="button button-no-upper lpv__gamebook">${text}</a>`,
    );
  }
}
