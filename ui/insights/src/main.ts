import menuHover from 'common/menuHover';
import { VNode, attributesModule, classModule, eventListenersModule, init, propsModule, styleModule } from 'snabbdom';
import { InsightOpts } from './types';
import InsightCtrl from './ctrl';
import view from './views/view';

const patch = init([classModule, attributesModule, styleModule, propsModule, eventListenersModule]);

export default function LishogiInsight(element: HTMLElement, opts: InsightOpts): void {
  let vnode: VNode, ctrl: InsightCtrl;

  const redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };

  ctrl = new InsightCtrl(opts, redraw);
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  vnode = patch(inner, view(ctrl));

  menuHover();
}
