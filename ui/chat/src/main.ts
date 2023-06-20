import { init, attributesModule, classModule } from 'snabbdom';

import makeCtrl from './ctrl';
import view from './view';
import { ChatOpts } from './interfaces';
import { PresetCtrl } from './preset';

export type { Ctrl as ChatCtrl, ChatPlugin } from './interfaces';

export function initModule(opts: ChatOpts): { preset: PresetCtrl } {
  const patch = init([classModule, attributesModule]);

  const ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.el.innerHTML = '';
  let vnode = patch(opts.el, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
