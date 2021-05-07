import { init, attributesModule, classModule } from 'snabbdom';

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts } from './interfaces';
import { PresetCtrl } from './preset';

export { Ctrl as ChatCtrl, ChatPlugin } from './interfaces';

export default function LichessChat(
  element: Element,
  opts: ChatOpts
): {
  preset: PresetCtrl;
} {
  const patch = init([classModule, attributesModule]);

  const ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
