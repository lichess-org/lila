import view from './view/main';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

import { MsgOpts } from './interfaces';
import { upgradeData } from './network';
import MsgCtrl from './ctrl';

export default function LichessMsg(opts: MsgOpts) {
  const element = document.querySelector('.msg-app') as HTMLElement,
    patch = init([klass, attributes]),
    appHeight = () => document.body.style.setProperty('--app-height', `${window.innerHeight}px`);
  window.addEventListener('resize', appHeight);
  appHeight();

  let vnode: VNode, ctrl: MsgCtrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new MsgCtrl(upgradeData(opts.data), lichess.trans(opts.i18n), redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
}
