/// <reference types="types/lichess" />
/// <reference types="types/lichess-jquery" />

import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

import { RoundOpts } from './interfaces';
import RoundController from './ctrl';
import { main as view } from './view/main';
import boot = require('./boot');

const patch = init([klass, attributes]);

export function app(opts: RoundOpts) {

  let vnode: VNode, ctrl: RoundController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new RoundController(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socket.receive,
    moveOn: ctrl.moveOn
  };
};

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
