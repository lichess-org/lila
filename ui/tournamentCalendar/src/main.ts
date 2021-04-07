import view from './view';

import { init, VNode, classModule, attributesModule } from 'snabbdom';

import { Ctrl } from './interfaces';

const patch = init([classModule, attributesModule]);

export function app(element: HTMLElement, env: any) {
  // enrich tournaments
  env.data.tournaments.forEach(t => {
    if (!t.bounds)
      t.bounds = {
        start: new Date(t.startsAt),
        end: new Date(t.startsAt + t.minutes * 60 * 1000),
      };
  });

  const ctrl: Ctrl = {
    data: env.data,
    trans: lichess.trans(env.i18n),
  };

  let vnode: VNode;
  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();
}
