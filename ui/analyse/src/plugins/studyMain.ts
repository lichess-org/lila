import { patch } from '../view/util';
import makeBoot from '../boot';
import makeStart from '../start';
import { Chessground } from 'chessground';
import * as studyDeps from '../study/studyDeps';

export { patch };

export const start = makeStart(patch, studyDeps);
export const boot = makeBoot(start);

export function initModule(cfg: any) {
  lichess.socket = new lichess.StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
  });
  cfg.socketSend = lichess.socket.send;
  const analyse = start(cfg);
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
