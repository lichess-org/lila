import exportLichessGlobals from "./site.lichess.globals"
import trans from './component/trans';

exportLichessGlobals();

window.onload = () => {

  document.body.classList.toggle('supports-max-content', !!window.chrome);

  const opts = window['analyseEmbedOpts'];
  window.LichessAnalyse.start({
    ...opts,
    socketSend: () => { },
    initialPly: 'url',
    trans: trans(opts.i18n)
  });

  window.addEventListener('resize', () =>
    document.body.dispatchEvent(new Event('chessground.resize'))
  );
}
