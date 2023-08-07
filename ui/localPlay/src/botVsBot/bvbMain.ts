import { attributesModule, classModule, init } from 'snabbdom';
import { BvbCtrl } from './bvbCtrl';
import bvbView from './bvbView';
import { BvbOpts } from './bvbInterfaces';
import { Chessground } from 'chessground';

const patch = init([classModule, attributesModule]);

export default async function (opts: BvbOpts) {
  const ctrl = new BvbCtrl(opts, redraw);

  const blueprint = bvbView(ctrl);
  const element = document.querySelector('main') as HTMLElement;
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, bvbView(ctrl));
  }
  redraw();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
