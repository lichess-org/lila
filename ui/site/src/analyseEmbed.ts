import exportNewChessGlobals from './site.lichess.globals';

exportNewChessGlobals();

export default function (opts: any) {
  window.NewChessAnalyse.start({
    ...opts,
    socketSend: () => {},
  });

  window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));

  if (opts.study?.chapter.gamebook)
    $('.main-board').append(
      $(
        `<a href="/study/${opts.study.id}/${opts.study.chapter.id}" target="_blank" rel="noopener" class="button gamebook-embed">Start</a>`
      )
    );
}
