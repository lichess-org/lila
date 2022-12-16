import menuHover from 'common/menuHover';
import { Shogiground } from 'shogiground';
import { VNode, attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from './ctrl';
import { Controller, PuzzleOpts } from './interfaces';
import view from './view/main';

const patch = init([classModule, attributesModule]);

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
