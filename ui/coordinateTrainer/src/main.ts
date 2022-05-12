import { init, attributesModule, eventListenersModule, classModule, propsModule, styleModule } from 'snabbdom';
import menuHover from 'common/menuHover';
import { Chessground } from 'chessground';

import view from './view';
import { CoordinateTrainerConfig } from './interfaces';
import CoordinateTrainerCtrl from './ctrl';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule, styleModule]);

export default function LichessCoordinateTrainer(element: HTMLElement, config: CoordinateTrainerConfig): void {
  const ctrl = new CoordinateTrainerCtrl(config, redraw);
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  let vnode = patch(inner, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
