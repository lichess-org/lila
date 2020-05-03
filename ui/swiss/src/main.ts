import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Chessground } from 'chessground';
import { SwissOpts, Redraw } from './interfaces';
import SwissController from './ctrl';
import * as chat from 'chat';

const patch = init([klass, attributes]);

import makeCtrl from './ctrl';
import view from './view/main';

export function start(opts: SwissOpts) {

  const element = document.querySelector('main.swiss') as HTMLElement;
  opts.classes = element.getAttribute('class');

  let vnode: VNode, ctrl: SwissController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new SwissController(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();

  return {
    socketReceive: ctrl.socket.receive
  };
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = chat;
