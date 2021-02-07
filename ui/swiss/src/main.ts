import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Chessground } from 'chessground';
import { SwissOpts } from './interfaces';
import SwissCtrl from './ctrl';
import LichessChat from 'chat';

const patch = init([klass, attributes]);

import view from './view/main';

export function start(opts: SwissOpts) {
  const element = document.querySelector('main.swiss') as HTMLElement;

  lichess.socket = new lichess.StrongSocket('/swiss/' + opts.data.id, opts.data.socketVersion || 0, {
    receive: (t: string, d: any) => ctrl.socket.receive(t, d),
  });
  opts.classes = element.getAttribute('class');
  opts.socketSend = lichess.socket.send;
  opts.element = element;
  opts.$side = $('.swiss__side').clone();

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new SwissCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = LichessChat;
