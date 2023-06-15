import { VNode, attributesModule, classModule, init } from 'snabbdom';
import { Ctrl } from './interfaces';
import view from './view';

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

  let vnode: VNode,
    ctrl: Ctrl = {
      data: env.data,
    };

  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();
}
