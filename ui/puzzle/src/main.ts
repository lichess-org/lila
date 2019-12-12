import makeCtrl from './ctrl';
import view from './view/main';

import { Chessground } from 'chessground';
import { Controller } from './interfaces';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { menuHover } from 'common/menuHover';

menuHover();

const patch = init([klass, attributes]);

export default function(opts) {

  let vnode: VNode, ctrl: Controller;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socketReceive
  };
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
