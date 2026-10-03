import { wsConnect } from 'lib/socket';

import type { AnalyseOpts } from '@/interfaces';

import makeBoot from './boot';
import makeStart from './start';
import { patch } from './view/util';

export { patch };

const start = makeStart(patch);
const boot = makeBoot(start);

export async function initModule({ mode, cfg }: { mode: 'userAnalysis' | 'replay'; cfg: AnalyseOpts }) {
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
