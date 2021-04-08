import view from './view';

import { init, VNode, classModule, attributesModule } from 'snabbdom';
import dragscroll from 'dragscroll';

const patch = init([classModule, attributesModule]);

dragscroll; // required to include the dependency :( :( :(

export default function (env: any) {
  lichess.StrongSocket.defaultParams.flag = 'tournament';

  const element = document.querySelector('.tour-chart') as HTMLElement;

  let vnode: VNode,
    ctrl = {
      data: () => env.data,
      trans: lichess.trans(env.i18n),
    };

  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  lichess.pubsub.on('socket.in.reload', d => {
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
