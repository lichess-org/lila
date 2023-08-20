import { patch } from './view/util';
import makeBoot from './boot';
import makeStart from './start';

export { patch };

export const start = makeStart(patch);

export const boot = makeBoot(start);

export function initModule({ mode, cfg }: { mode: 'userAnalysis' | 'replay'; cfg: any }) {
  if (mode === 'replay') boot(cfg);
  else userAnalysis(cfg);
}

function userAnalysis(cfg: any) {
  cfg.$side = $('.analyse__side').clone();
  lichess.socket = new lichess.StrongSocket(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
  });
  cfg.socketSend = lichess.socket.send;
  const analyse = start(cfg);
}
