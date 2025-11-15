import view from './view/calendarView';

import { init, type VNode, classModule, attributesModule } from 'snabbdom';
import type { Tournament } from './interfaces';

const patch = init([classModule, attributesModule]);

export type Lanes = Array<Array<Tournament>>;

export interface Data {
  since: number;
  to: number;
  tournaments: Tournament[];
}

export interface Ctrl {
  data: Data;
}

export function initModule(opts: { data: Data }) {
  const element = document.getElementById('tournament-calendar');
  // enrich tournaments
  opts.data.tournaments.forEach(t => {
    if (!t.bounds)
      t.bounds = {
        start: new Date(t.startsAt),
        end: new Date(t.startsAt + t.minutes * 60 * 1000),
      };
  });

  const ctrl: Ctrl = {
    data: opts.data,
  };

  let vnode: VNode;
  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();
}
