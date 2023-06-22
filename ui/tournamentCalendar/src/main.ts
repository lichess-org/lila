import view from './view';

import { init, VNode, classModule, attributesModule } from 'snabbdom';

import { Ctrl, Opts } from './interfaces';

const patch = init([classModule, attributesModule]);

export function initModule(opts: Opts) {
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
