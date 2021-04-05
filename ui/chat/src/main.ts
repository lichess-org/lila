import { init, attributesModule, classModule, VNode } from 'snabbdom';

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts, Ctrl } from './interfaces';
import { PresetCtrl } from './preset';

export { Ctrl as ChatCtrl, ChatPlugin } from './interfaces';

export default function LichessChat(
  element: Element,
  opts: ChatOpts
): {
  preset: PresetCtrl;
} {
  const patch = init([classModule, attributesModule]);

  let vnode: VNode, ctrl: Ctrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  return ctrl;
}
