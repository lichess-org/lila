import menuHover from 'common/menuHover';
import { Shogiground } from 'shogiground';
import { VNode, attributesModule, classModule, eventListenersModule, init, propsModule, styleModule } from 'snabbdom';
import LearnCtrl from './ctrl';
import { LearnOpts } from './interfaces';
import view from './views/view';

const patch = init([classModule, attributesModule, styleModule, propsModule, eventListenersModule]);

export default function LishogiLearn(element: HTMLElement, opts: LearnOpts): void {
  let vnode: VNode, ctrl: LearnCtrl;

  const redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };

  ctrl = new LearnCtrl(opts, redraw);
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  vnode = patch(inner, view(ctrl));

  menuHover();
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
