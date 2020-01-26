import view from './view/main';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

import { MsgOpts } from './interfaces'
import MsgCtrl from './ctrl';

const patch = init([klass, attributes]);

export default function LichessMsg(element: HTMLElement, opts: MsgOpts) {

  let vnode: VNode, ctrl: MsgCtrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new MsgCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
};
