import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { Chessground } from 'chessground';
import { StormOpts } from './interfaces';
import StormCtrl from './ctrl';

const patch = init([klass, attributes]);

import view from './view/main';

export function start(opts: StormOpts) {

  const element = document.querySelector('main.storm') as HTMLElement;

  let vnode: VNode;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  const ctrl = new StormCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  redraw();
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
