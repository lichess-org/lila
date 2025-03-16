import { init, classModule, attributesModule } from 'snabbdom';

import { BotOpts } from './interfaces';
import { BotCtrl } from './botCtrl';

export async function initModule(opts: BotOpts) {
  const element = document.querySelector('main.bot-play') as HTMLElement,
    patch = init([classModule, attributesModule]);

  const ctrl = new BotCtrl(opts, redraw);

  const blueprint = ctrl.view();
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, ctrl.view());
  }

  redraw();
}
