import view from './view/main';

import { init, classModule, attributesModule } from 'snabbdom';

import { MsgOpts } from './interfaces';
import { upgradeData } from './network';
import MsgCtrl from './ctrl';

export default function LichessMsg(opts: MsgOpts) {
  const element = document.querySelector('.msg-app') as HTMLElement,
    patch = init([classModule, attributesModule]),
    appHeight = () => document.body.style.setProperty('--app-height', `${window.innerHeight}px`);
  window.addEventListener('resize', appHeight);
  appHeight();

  const ctrl = new MsgCtrl(upgradeData(opts.data), lichess.trans(opts.i18n), redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();
}
