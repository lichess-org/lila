import { init, classModule, attributesModule, eventListenersModule } from 'snabbdom';

import { BotCtrl } from './botCtrl';
import type { BotOpts } from './interfaces';

export async function initModule(opts: BotOpts) {
  const element = document.querySelector('main.bot-play') as HTMLElement,
    patch = init([classModule, attributesModule, eventListenersModule]);

  const ctrl = new BotCtrl(opts, redraw);

  let vnode = patch(element, ctrl.view());

  const mainWrap = document.getElementById('main-wrap')!;

  function redraw() {
    mainWrap.classList.toggle('bot-play', !!ctrl.playCtrl);
    vnode = patch(vnode, ctrl.view());
  }

  redraw();
}
