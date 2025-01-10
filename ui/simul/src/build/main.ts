import { type VNode, attributesModule, classModule, init } from 'snabbdom';
import { boot } from '../boot';
import SimulCtrl from '../ctrl';
import type { SimulOpts } from '../interfaces';
import view from '../view/main';

const patch = init([classModule, attributesModule]);

function main(opts: SimulOpts): SimulCtrl {
  return boot(opts, start);
}

function start(opts: SimulOpts): SimulCtrl {
  const element = document.querySelector('main.simul') as HTMLElement;

  const ctrl = new SimulCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode: VNode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  redraw();

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);
