import { wsConnect } from 'lib/socket';

import type { AnalyseOpts } from '../interfaces';
import type { AnalyseSocketSend } from '../socket';
import makeStart from '../start';
import { patch } from '../view/util';
import * as studyDeps from './studyDeps';

export { patch };

const start = makeStart(patch, studyDeps);

export async function initModule(cfg: AnalyseOpts) {
  await site.asset.loadPieces;
  cfg.socketSend = wsConnect(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion ?? false, {
    options: { reloadOnResume: true },
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
    ...(cfg.embed ? { params: { flag: 'embed' } } : {}),
  }).send as AnalyseSocketSend;
  const analyse = start(cfg);
}
