import view from './view/scheduleView';

import { init, type VNode, classModule, attributesModule } from 'snabbdom';
import type { Tournament } from './interfaces';
import { wsConnect } from 'common/socket';
import { pubsub } from 'common/pubsub';

const patch = init([classModule, attributesModule]);

export type Lane = Tournament[];

export interface Data {
  created: Tournament[];
  started: Tournament[];
  finished: Tournament[];
}
export interface Ctrl {
  data(): Data;
}

export function initModule(opts: { data: Data }) {
  wsConnect('/socket/v5', false, { params: { flag: 'tournament' } });

  const element = document.querySelector('.tour-chart') as HTMLElement;

  const ctrl = {
    data: () => opts.data,
  };

  let vnode: VNode;
  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  pubsub.on('socket.in.reload', d => {
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
