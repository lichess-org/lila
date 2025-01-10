import {
  attributesModule,
  classModule,
  eventListenersModule,
  init,
  propsModule,
  styleModule,
} from 'snabbdom';
import InsightCtrl from '../ctrl';
import type { InsightOpts } from '../types';
import view from '../views/view';

const patch = init([classModule, attributesModule, styleModule, propsModule, eventListenersModule]);

function main(opts: InsightOpts): void {
  const element = document.querySelector('insights-app--wrap')!;

  const ctrl = new InsightCtrl(opts, redraw);

  element.innerHTML = '';
  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
}

window.lishogi.registerModule(__bundlename__, main);
