import view from './view';

import { init, VNode, classModule, attributesModule } from 'snabbdom';
import dragscroll from 'dragscroll';
import { Opts, Tournament } from './interfaces';

const patch = init([classModule, attributesModule]);

dragscroll; // required to include the dependency :( :( :(

export default function (opts: Opts) {
  lichess.StrongSocket.defaultParams.flag = 'tournament';

  const element = document.querySelector('.tour-chart') as HTMLElement;

  const ctrl = {
    data: () => opts.data,
    trans: lichess.trans(opts.i18n),
  };

  let vnode: VNode;
  function redraw() {
    vnode = patch(vnode || element, view(ctrl));
  }

  redraw();

  setInterval(redraw, 3700);

  lichess.pubsub.on('socket.in.reload', d => {
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
