import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom';
import { classModule } from 'snabbdom';
import { attributesModule } from 'snabbdom';

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

  let vnode: VNode,
    ctrl: Ctrl = {
      data: env.data,
      trans: lichess.trans(env.i18n),
    };

  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();
}
