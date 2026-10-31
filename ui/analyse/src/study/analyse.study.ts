import { pubsub } from 'lib/pubsub';
import { wsConnect } from 'lib/socket';
import { alert } from 'lib/view';

import type { AnalyseOpts } from '../interfaces';
import type { AnalyseSocketSend } from '../socket';
import makeStart from '../start';
import { patch } from '../view/util';
import * as studyDeps from './studyDeps';
import { StudyNavigation } from './studyNavigation';

export { patch };

const start = makeStart(patch, studyDeps);

export async function initModule(cfg: AnalyseOpts) {
  await site.asset.loadPieces;
  const connect = (opts: AnalyseOpts) => {
    opts.socketSend = wsConnect(opts.socketUrl || '/analysis/socket/v5', opts.socketVersion ?? false, {
      options: { reloadOnResume: true },
      receive: (t: string, d: any) => analyse.socketReceive(t, d),
      ...(opts.embed ? { params: { flag: 'embed' } } : {}),
    }).send as AnalyseSocketSend;
  };
  connect(cfg);
  const analyse = start(cfg);
  const navigation = new StudyNavigation(cfg, analyse, connect);
  pubsub.on('study.navigate', request => {
    navigation.load(request.studyId, request.chapterId, request).then(
      () => request.complete?.(),
      error => {
        const failure = error instanceof Error ? error : new Error(String(error));
        if (request.complete) request.complete(failure);
        else alert(failure.message);
      },
    );
  });
}
