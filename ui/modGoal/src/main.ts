import { VNode, init, attributesModule, eventListenersModule, classModule } from 'snabbdom';
import Ctrl from './ctrl';
import view from './view';

const patch = init([classModule, attributesModule, eventListenersModule]);

export default function LichessModGoal() {
  let vnode: VNode, ctrl: Ctrl;

  const element = $('<div class="mod-goal">').appendTo(document.body)[0];

  const redraw = () => {
    vnode = patch(vnode || element, view(ctrl));
  };
  ctrl = new Ctrl(redraw);
  redraw();
}
