import Lpv from 'lichess-pgn-viewer';
import { Opts } from 'lichess-pgn-viewer/interfaces';

interface OptsWithI18n extends Opts {
  i18n: any;
}

export default function start(elem: HTMLElement, opts: OptsWithI18n) {
  Lpv(elem, {
    ...opts,
    fullScreen: true,
    pgn: elem.innerHTML,
    translate: key => opts.i18n[key] || key,
  });
}
