import exportLichessGlobals from './site.lichess.globals';

exportLichessGlobals();

export default function (opts: any) {
  document.body.classList.toggle('supports-max-content', !!window.chrome);

  window.LichessAnalyse.start({
    ...opts,
    socketSend: () => {},
  });

  window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));
}
