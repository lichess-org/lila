import view from './view/scheduleView';

import { init, VNode, classModule, attributesModule } from 'snabbdom';
import { Tournament } from './interfaces';

const patch = init([classModule, attributesModule]);

export type Lane = Tournament[];

export interface Data {
  created: Tournament[];
  started: Tournament[];
  finished: Tournament[];
}
export interface Ctrl {
  data(): Data;
  trans: Trans;
}

export function initModule(opts: { data: Data; i18n: I18nDict }) {
  site.StrongSocket.defaultParams.flag = 'tournament';

  const element = document.querySelector('.tour-chart') as HTMLElement;

  const ctrl = {
    data: () => opts.data,
    trans: site.trans(opts.i18n),
  };

  let vnode: VNode;
  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  site.pubsub.on('socket.in.reload', d => {
    opts.data = {
      created: update(opts.data.created, d.created),
      started: update(opts.data.started, d.started),
      finished: update(opts.data.finished, d.finished),
    };
    redraw();
  });
}

function update(prevs: Tournament[], news: Tournament[]) {
  // updates ignore team tournaments (same for all)
  // also lacks finished tournaments
  const now = new Date().getTime();
  return news.concat(prevs.filter(p => !p.schedule || p.finishesAt < now));
}
