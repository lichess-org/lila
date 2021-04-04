import { attributesModule } from 'snabbdom';
import { classModule } from 'snabbdom';
import menuHover from 'common/menuHover';
import StormCtrl from './ctrl';
import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { StormOpts } from './interfaces';
import { VNode } from 'snabbdom';

const patch = init([classModule, attributesModule]);

import view from './view/main';

export function start(opts: StormOpts) {
  const element = document.querySelector('.storm-app') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new StormCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  menuHover();
  $('script').remove();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
