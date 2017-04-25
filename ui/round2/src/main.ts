import makeCtrl = require('./ctrl');
import view = require('./view/main');
import boot = require('./boot');

import { Chessground } from 'chessground';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

const patch = init([klass, attributes]);

export function mithril(opts: any) {

  let vnode: VNode, ctrl: any;

  function redraw() {
    vnode = patch(vnode, view.main(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  vnode = patch(opts.element, view.main(ctrl));

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn
  };
};

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
