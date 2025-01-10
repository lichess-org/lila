import menuSlowdown from 'common/menu-slowdown';
import { Shogiground } from 'shogiground';
import {
  attributesModule,
  classModule,
  eventListenersModule,
  init,
  propsModule,
  styleModule,
} from 'snabbdom';
import LearnCtrl from '../ctrl';
import type { LearnOpts } from '../interfaces';
import view from '../views/view';

const patch = init([classModule, attributesModule, styleModule, propsModule, eventListenersModule]);

function main(opts: LearnOpts): LearnCtrl {
  const element = document.querySelector('.learn-app--wrap')!;

  const ctrl = new LearnCtrl(opts, redraw);

  let vnode = patch(element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuSlowdown();

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);

window.Shogiground = Shogiground;
