import { attributesModule, classModule, init, propsModule, styleModule } from 'snabbdom';

import menuHover from 'lib/menuHover';
import { mountApp } from 'lib/view';

import RacerCtrl from './ctrl';
import type { RacerOpts } from './interfaces';

const patch = init([classModule, attributesModule, propsModule, styleModule]);

import view from './view/main';

export async function initModule(opts: RacerOpts) {
  await site.asset.loadPieces;
  const element = document.querySelector('.racer-app') as HTMLElement;

  element.innerHTML = '';
  mountApp({
    el: element,
    patch,
    makeCtrl: redraw => new RacerCtrl(opts, redraw),
    view,
  });

  menuHover();
  $('script').remove();
}
