import { attributesModule, classModule, init } from 'snabbdom';
import MsgCtrl from '../ctrl';
import { MsgOpts } from '../interfaces';
import { upgradeData } from '../network';
import view from '../view/main';

const patch = init([classModule, attributesModule]);

function main(opts: MsgOpts): MsgCtrl {
  const element = document.querySelector('.msg-app')!;

  heightHack();

  const ctrl = new MsgCtrl(upgradeData(opts.data), redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}

function heightHack() {
  const appHeight = () =>
    document.body.style.setProperty('--app-height', `${window.innerHeight}px`);
  window.addEventListener('resize', appHeight);
  appHeight();
}

window.lishogi.registerModule(__bundlename__, main);
