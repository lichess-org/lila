import attributes from 'snabbdom/modules/attributes';
import klass from 'snabbdom/modules/class';
import makeCtrl from './ctrl';
import { menuHover } from 'common/menuHover';
import view from './view/main';
import { Shogiground } from 'shogiground';
import { Controller, PuzzleOpts } from './interfaces';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

const patch = init([klass, attributes]);

export default function (opts: PuzzleOpts): void {
  const element = document.querySelector('main.puzzle') as HTMLElement;
  let vnode: VNode, ctrl: Controller;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  vnode = patch(element, blueprint);

  menuHover();
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
