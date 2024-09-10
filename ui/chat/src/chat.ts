import { init, attributesModule, classModule } from 'snabbdom';

import view from './view';
import { ChatOpts } from './interfaces';
import ChatCtrl from './ctrl';

export type { ChatPlugin } from './interfaces';
export { default as ChatCtrl } from './ctrl';

export function makeChat(data: Partial<ChatOpts>): ChatCtrl {
  const opts = { el: document.querySelector('.mchat'), ...data } as ChatOpts;
  const patch = init([classModule, attributesModule]);

  const ctrl = new ChatCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.el.innerHTML = '';
  let vnode = patch(opts.el, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  return ctrl;
}
