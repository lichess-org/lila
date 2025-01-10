import menuSlowdown from 'common/menu-slowdown';
import { Shogiground } from 'shogiground';
import { attributesModule, classModule, init } from 'snabbdom';
import { boot } from '../boot';
import RoundController from '../ctrl';
import type { RoundOpts } from '../interfaces';
import { main as view } from '../view/main';

const patch = init([classModule, attributesModule]);

function main(opts: RoundOpts): RoundController {
  return boot(opts, start);
}

function start(element: HTMLElement, opts: RoundOpts): RoundController {
  const ctrl = new RoundController(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  window.addEventListener('resize', redraw); // col1 / col2+ transition

  if (ctrl.isPlaying()) menuSlowdown();

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);

window.Shogiground = Shogiground;
