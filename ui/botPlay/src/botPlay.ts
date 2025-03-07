import { init, classModule, attributesModule } from 'snabbdom';

import { BotOpts } from './interfaces';
import { BotPlayCtrl } from './ctrl';
import { Assets } from 'local/assets';

export async function initModule(opts: BotOpts) {
  const element = document.querySelector('.bot-play-app') as HTMLElement,
    patch = init([classModule, attributesModule]);

  const assets = new Assets();
  // await Promise.all([assets.init()]);
  const ctrl = new BotPlayCtrl(opts, assets, redraw);

  const blueprint = ctrl.view();
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, ctrl.view());
  }

  redraw();
}
