import { patch } from './view/util';
import makeBoot from './boot';
import makeStart from './start';
import { wsConnect } from 'lib/socket';

export { patch };

const start = makeStart(patch);
const boot = makeBoot(start);

export async function initModule({ mode, cfg }: { mode: 'userAnalysis' | 'replay'; cfg: any }) {
  await site.asset.loadPieces;
  if (mode === 'replay') boot(cfg);
  else userAnalysis(cfg);
}

function userAnalysis(cfg: any) {
  cfg.$side = $('.analyse__side').clone();
  cfg.socketSend = wsConnect(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
  }).send;
  const analyse = start(cfg);
}
