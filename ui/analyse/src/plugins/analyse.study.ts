import { patch } from '../view/util';
import makeBoot from '../boot';
import makeStart from '../start';
import * as studyDeps from '../study/studyDeps';
import { wsConnect } from 'common/socket';

export { patch };

export const start = makeStart(patch, studyDeps);
export const boot = makeBoot(start);

export function initModule(cfg: any) {
  cfg.socketSend = wsConnect(cfg.socketUrl || '/analysis/socket/v5', cfg.socketVersion, {
    receive: (t: string, d: any) => analyse.socketReceive(t, d),
    ...(cfg.embed ? { params: { flag: 'embed' } } : {}),
  }).send;
  const analyse = start(cfg);
}
