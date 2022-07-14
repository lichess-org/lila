import ReplayCtrl from './ctrl';
import view from './view';
import { init, attributesModule, classModule, eventListenersModule } from 'snabbdom';
import { ReplayOpts } from './interfaces';

export default function start(element: Element, opts: ReplayOpts) {
  const patch = init([classModule, attributesModule, eventListenersModule]);

  const ctrl = new ReplayCtrl(opts, redraw);

  const blueprint = view(ctrl);
  element.innerHTML = '';
  let vnode = patch(element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }
}
