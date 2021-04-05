import view from './view/main';

import { init, VNode, classModule, attributesModule } from 'snabbdom';

import { MsgOpts } from './interfaces';
import { upgradeData } from './network';
import MsgCtrl from './ctrl';

export default function LichessMsg(opts: MsgOpts) {
  const element = document.querySelector('.msg-app') as HTMLElement,
    patch = init([classModule, attributesModule]),
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
