import menuHover from 'lib/menuHover';
import { mountApp } from 'lib/view';

import StormCtrl from './ctrl';
import type { StormOpts } from './interfaces';
import view from './view/main';

export async function initModule(opts: StormOpts): Promise<void> {
  await site.asset.loadPieces;
  const element = document.querySelector('.storm-app') as HTMLElement;

  element.innerHTML = '';
  mountApp({
    el: element,
    makeCtrl: redraw => new StormCtrl(opts, redraw),
    view,
  });

  menuHover();
  $('script').remove();
}
