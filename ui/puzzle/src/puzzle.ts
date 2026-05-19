import { attributesModule, classModule, init } from 'snabbdom';

import menuHover from 'lib/menuHover';

import PuzzleCtrl from './ctrl';
import type { PuzzleOpts, NvuiPlugin } from './interfaces';
import view from './view/main';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: PuzzleOpts) {
  await site.asset.loadPieces;
  const element = document.querySelector('main.puzzle') as HTMLElement;
  const ctrl = new PuzzleCtrl(opts, redraw);
  const nvui = site.blindMode && (await site.asset.loadEsm<NvuiPlugin>('puzzle.nvui', { init: ctrl }));
  const render = nvui ? nvui.render : () => view(ctrl);

  const blueprint = render();
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, render());
  }

  menuHover();
}
