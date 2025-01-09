import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from './ctrl';
import { ChatOpts, ChatCtrl } from './interfaces';
import view from './view';

export function makeChat(opts: ChatOpts): ChatCtrl {
  const patch = init([classModule, attributesModule]),
    element = document.querySelector('.mchat')!,
    ctrl = makeCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
