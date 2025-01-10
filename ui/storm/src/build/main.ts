import menuSlowdown from 'common/menu-slowdown';
import { Shogiground } from 'shogiground';
import { attributesModule, classModule, init } from 'snabbdom';
import StormCtrl from '../ctrl';
import type { StormOpts } from '../interfaces';
import view from '../view/main';

const patch = init([classModule, attributesModule]);

function main(opts: StormOpts): void {
  const element = document.querySelector('.storm storm-app')!;

  const ctrl = new StormCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuSlowdown();
}

window.lishogi.registerModule(__bundlename__, main);

window.Shogiground = Shogiground;
