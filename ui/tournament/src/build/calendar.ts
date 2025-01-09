import { VNode, attributesModule, classModule, init } from 'snabbdom';
import { Ctrl } from '../calendar/interfaces';
import view from '../calendar/view';

const patch = init([classModule, attributesModule]);

function main(env: any): void {
  const element = document.getElementById('tournament-calendar');
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

window.lishogi.registerModule(__bundlename__, main);
