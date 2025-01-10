import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from './ctrl';
import type { ChatCtrl, ChatOpts } from './interfaces';
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
