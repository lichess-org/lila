import { patch } from '../view/util';
import makeBoot from '../boot';
import makeStart from '../start';
import { Chessground } from 'chessground';
import * as studyDeps from '../study/studyDeps';

export { patch };

export const start = makeStart(patch, studyDeps);
export const boot = makeBoot(start);

export function initModule({ mode, cfg }: { mode: 'practice' | 'relay' | 'study' | 'embed'; cfg: any }) {
  if (mode === 'embed') embed(cfg);
  else study(cfg);
}

function embed(cfg: any) {
  start({ ...cfg, socketSend: () => {} });
  window.addEventListener('resize', () => document.body.dispatchEvent(new Event('chessground.resize')));
  if (cfg.study?.chapter.gamebook) {
    $('.main-board').append(
      $(
        `<a href="/study/${cfg.study.id}/${cfg.study.chapter.id}" target="_blank" rel="noopener" class="button gamebook-embed">Start</a>`
      )
    );
  }
  document.getElementById('chapter-selector')!.onchange = function (this: HTMLSelectElement) {
    console.log(this.value);
    location.href = this.value + location.search;
  };
}

function study(cfg: any) {
  lichess.socket = new lichess.StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
  });
  cfg.socketSend = lichess.socket.send;
  const analyse = start(cfg);
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
