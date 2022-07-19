import { EditorData } from './interfaces';
import EditorCtrl from './ctrl';
import view from './view';

import { init, VNode, classModule, attributesModule, propsModule, eventListenersModule } from 'snabbdom';

import menuHover from 'common/menuHover';
import { Shogiground } from 'shogiground';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export default function LishogiEditor(element: HTMLElement, data: EditorData) {
  let vnode: VNode, ctrl: EditorCtrl;

  const redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };

  ctrl = new EditorCtrl(data, redraw);
  element.innerHTML = '';
  const inner = document.createElement('div');
  element.appendChild(inner);
  vnode = patch(inner, view(ctrl));

  menuHover();

  return {
    getSfen: ctrl.getSfen.bind(ctrl),
    setOrientation: ctrl.setOrientation.bind(ctrl),
  };
}

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
