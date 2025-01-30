import { attributesModule, classModule, init } from 'snabbdom';
import view from '../schedule/view';

const patch = init([classModule, attributesModule]);

function main(env: any) {
  window.lishogi.socket = new window.lishogi.StrongSocket('/socket/v5', false, {
    params: { flag: 'tournament' },
  });
  env.element = document.querySelector('.tour-chart') as HTMLElement;

  const ctrl = {
    data: () => env.data,
  };
  let vnode = patch(env.element, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  window.lishogi.pubsub.on('socket.in.reload', d => {
    env.data = {
      created: update(env.data.created, d.created),
      started: update(env.data.started, d.started),
      finished: update(env.data.finished, d.finished),
    };
    redraw();
  });
}

function update(prevs, news) {
  // updates ignore team tournaments (same for all)
  // also lacks finished tournaments
  const now = new Date().getTime();
  return news.concat(prevs.filter(p => !p.schedule || p.finishesAt < now));
}

window.lishogi.registerModule(__bundlename__, main);
