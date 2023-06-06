import { attributesModule, classModule, init } from 'snabbdom';
import menuHover from 'common/menuHover';
import view from './view/main';
import { Chessground } from 'chessground';
import { PuzzleOpts } from './interfaces';
import PuzzleController from './ctrl';

const patch = init([classModule, attributesModule]);

export default (window as any).LichessPuzzle = function (opts: PuzzleOpts): void {
  const element = document.querySelector('main.puzzle') as HTMLElement;
  const ctrl = new PuzzleController(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
