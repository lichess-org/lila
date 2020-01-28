import view from './view/main';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

import { MsgOpts } from './interfaces'
import { upgradeData } from './network'
import MsgCtrl from './ctrl';

const patch = init([klass, attributes]);

export default function LichessMsg(element: HTMLElement, opts: MsgOpts) {

  let vnode: VNode, ctrl: MsgCtrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new MsgCtrl(
    upgradeData(opts.data),
    window.lichess.trans(opts.i18n),
    redraw
  );

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
};
