import { attributesModule, classModule, init, type VNode } from 'snabbdom';
import menuHover from 'lib/menuHover';
import RacerCtrl from './ctrl';
import type { RacerOpts } from './interfaces';

const patch = init([classModule, attributesModule]);

import view from './view/main';

export async function initModule(opts: RacerOpts) {
  await site.asset.loadPieces;
  const element = document.querySelector('.racer-app') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new RacerCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  menuHover();
  $('script').remove();
}
