import { attributesModule, classModule, init } from 'snabbdom';
import { Ctrl } from './ctrl';
import view from './view';
import { LocalPlayOpts, Controller } from './interfaces';
import menuHover from 'common/menuHover';
import { Chessground } from 'chessground';

const patch = init([classModule, attributesModule]);

export async function initModule(opts: LocalPlayOpts) {
  // make a StrongSocket
  const ctrl = new Ctrl(opts, redraw);

  const blueprint = view(ctrl as Controller);
  const element = document.querySelector('main') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
  redraw();
  menuHover();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
