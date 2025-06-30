import { attributesModule, classModule, init } from 'snabbdom';
import PuzzleCtrl from './ctrl';
import menuHover from 'lib/menuHover';
import view from './view/main';
import type { PuzzleOpts, NvuiPlugin } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: PuzzleOpts) {
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
