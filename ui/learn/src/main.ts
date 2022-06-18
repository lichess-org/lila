import { LearnOpts } from './interfaces';
import LearnCtrl from './ctrl';
import view from './views/view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import styles from 'snabbdom/modules/style';
import props from 'snabbdom/modules/props';
import eventlisteners from 'snabbdom/modules/eventlisteners';

import { menuHover } from 'common/menuHover';
import { Shogiground } from 'shogiground';

const patch = init([klass, attributes, styles, props, eventlisteners]);

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
