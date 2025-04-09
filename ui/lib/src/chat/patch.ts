import { init, attributesModule, classModule } from 'snabbdom';

import { renderChat } from './renderChat';
import type { ChatOpts } from './interfaces';
import { ChatCtrl } from './chatCtrl';

export function makeChatWithPatch(data: Partial<ChatOpts>): ChatCtrl {
  const opts = { el: document.querySelector('.mchat')!, ...data } as { el: HTMLElement } & ChatOpts;
  const patch = init([classModule, attributesModule]);

  const ctrl = new ChatCtrl(opts, redraw);

  const blueprint = renderChat(ctrl);
  opts.el.innerHTML = '';
  let vnode = patch(opts.el, blueprint);

  function redraw() {
    vnode = patch(vnode, renderChat(ctrl));
  }

  return ctrl;
}
