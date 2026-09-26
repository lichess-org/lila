import {
  init,
  attributesModule,
  eventListenersModule,
  classModule,
  propsModule,
  styleModule,
} from 'snabbdom';

import menuHover from 'lib/menuHover';

import CoordinateTrainerCtrl from './ctrl';
import type { CoordinateTrainerConfig } from './interfaces';
import view from './view';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule, styleModule]);

export function initModule(config: CoordinateTrainerConfig) {
  const ctrl = new CoordinateTrainerCtrl(config, redraw);
  const element = document.getElementById('trainer')!;
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  let vnode = patch(inner, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();
}
