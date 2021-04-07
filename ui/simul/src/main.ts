import { init, VNode, classModule, attributesModule } from 'snabbdom';
import { SimulOpts } from './interfaces';
import SimulCtrl from './ctrl';
import LichessChat from 'chat';

const patch = init([classModule, attributesModule]);

import view from './view/main';

export function start(opts: SimulOpts) {
  const element = document.querySelector('main.simul') as HTMLElement;

  lichess.socket = new lichess.StrongSocket(`/simul/${opts.data.id}/socket/v4`, opts.socketVersion, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  });
  opts.socketSend = lichess.socket.send;
  opts.element = element;
  opts.$side = $('.simul__side').clone();

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new SimulCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
}

// that's for the rest of lichess to access the chat
window.LichessChat = LichessChat;
