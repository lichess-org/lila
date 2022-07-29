import Lpv from 'lichess-pgn-viewer';
import { Opts } from 'lichess-pgn-viewer/interfaces';

interface OptsWithI18n extends Opts {
  i18n: any;
}

export default function start(elem: HTMLElement, opts: OptsWithI18n) {
  const i18n = {
    ...opts.i18n,
    flipTheBoard: opts.i18n.flipBoard,
    analysisBoard: opts.i18n.analysis,
  };
  Lpv(elem, {
    ...opts,
    showMoves: 'auto',
    pgn: elem.innerHTML,
    translate: key => i18n[key],
  });
}
