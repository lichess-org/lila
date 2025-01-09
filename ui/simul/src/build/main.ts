import { init, VNode, classModule, attributesModule } from 'snabbdom';
import { SimulOpts } from '../interfaces';
import SimulCtrl from '../ctrl';
import view from '../view/main';
import { boot } from '../boot';

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
