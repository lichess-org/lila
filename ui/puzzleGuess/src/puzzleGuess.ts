import { attributesModule, classModule, init, type VNode } from 'snabbdom';

import PuzzleGuessCtrl from './ctrl';
import type { PuzzleGuessOpts } from './interfaces';
import view from './view';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: PuzzleGuessOpts): Promise<void> {
  await site.asset.loadPieces;
  const element = document.querySelector('.puzzle-guess-app') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new PuzzleGuessCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);
}
