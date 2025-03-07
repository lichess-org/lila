import { init, classModule, attributesModule } from 'snabbdom';

import { BotOpts } from './interfaces';
import { BotPlay } from './botPlay';

export function initModule(opts: BotOpts) {
  const element = document.querySelector('.bot-play-app') as HTMLElement,
    patch = init([classModule, attributesModule]);

  const ctrl = new BotPlay(opts, redraw);

  const blueprint = ctrl.view();
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, ctrl.view());
  }

  redraw();
}
