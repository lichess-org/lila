import menuSlowdown from 'common/menu-slowdown';
import { Shogiground } from 'shogiground';
import { attributesModule, classModule, eventListenersModule, init, propsModule } from 'snabbdom';
import EditorCtrl from '../ctrl';
import { EditorData } from '../interfaces';
import view from '../view/view';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

function main(data: EditorData): EditorCtrl {
  data.element = data.element || document.querySelector<HTMLElement>('.board-editor')!;

  const ctrl = new EditorCtrl(data, redraw);

  data.element.innerHTML = '';
  let vnode = patch(data.element, view(ctrl));
  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  if (!data.embed) menuSlowdown();

  return ctrl;
}

window.lishogi.registerModule(__bundlename__, main);

window.Shogiground = Shogiground;
