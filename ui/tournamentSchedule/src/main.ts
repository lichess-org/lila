import dragscroll from 'dragscroll';
import { VNode, attributesModule, classModule, init } from 'snabbdom';
import view from './view';

const patch = init([classModule, attributesModule]);

dragscroll; // required to include the dependency :( :( :(

export function app(element: HTMLElement, env: any) {
  let vnode: VNode,
    ctrl = {
      data: () => env.data,
      trans: window.lishogi.trans(env.i18n),
    };

  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  return {
    update: d => {
      (env.data = {
        created: update(env.data.created, d.created),
        started: update(env.data.started, d.started),
        finished: update(env.data.finished, d.finished),
      }),
        redraw();
    },
  };
}

function update(prevs, news) {
  // updates ignore team tournaments (same for all)
  // also lacks finished tournaments
  const now = new Date().getTime();
  return news.concat(prevs.filter(p => !p.schedule || p.finishesAt < now));
}
