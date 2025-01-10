import menuSlowdown from 'common/menu-slowdown';
import { Shogiground } from 'shogiground';
import { attributesModule, classModule, init } from 'snabbdom';
import makeCtrl from '../ctrl';
import type { Controller, PuzzleOpts } from '../interfaces';
import view from '../view/main';

const patch = init([classModule, attributesModule]);

function main(opts: PuzzleOpts): Controller {
  const element = document.querySelector('main.puzzle') as HTMLElement;

  const ctrl = makeCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuSlowdown();

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);

window.Shogiground = Shogiground;
