import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from './ctrl';
import menuHover from 'common/menuHover';
import view from './view/main';
import { PuzzleOpts, NvuiPlugin } from './interfaces';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: PuzzleOpts) {
  const element = document.querySelector('main.puzzle') as HTMLElement;
  const nvui = lichess.blindMode ? await lichess.loadEsm<NvuiPlugin>('puzzle.nvui') : undefined;
  const ctrl = { ...makeCtrl(opts, redraw), nvui };

  const blueprint = view(ctrl);
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();
}
