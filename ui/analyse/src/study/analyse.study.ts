import { patch } from '../view/util';
import makeStart from '../start';
import type { AnalyseOpts } from '../interfaces';
import type { AnalyseSocketSend } from '../socket';
import * as studyDeps from './studyDeps';
import { wsConnect } from 'lib/socket';

export { patch };

const start = makeStart(patch, studyDeps);

export function initModule(cfg: AnalyseOpts) {
  cfg.socketSend = wsConnect(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion ?? false, {
    options: { reloadOnResume: true },
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
    ...(cfg.embed ? { params: { flag: 'embed' } } : {}),
  }).send as AnalyseSocketSend;
  const analyse = start(cfg);
}
