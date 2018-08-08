import makeCtrl from './ctrl';
import view from './view/main';
import sideView from './view/side';

import { Draughtsground } from 'draughtsground';
import { Controller } from './interfaces';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

const patch = init([klass, attributes]);

export default function(opts) {

  let vnode: VNode, sideVnode: VNode, ctrl: Controller;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
    sideVnode = patch(sideVnode, sideView(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  sideVnode = patch(opts.sideElement, sideView(ctrl));

  return {
    socketReceive: ctrl.socketReceive
  };
};

// that's for the rest of lidraughts to access draughtsground
// without having to include it a second time
window.Draughtsground = Draughtsground;
