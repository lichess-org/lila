import EditorCtrl from './ctrl';
import menuHover from 'common/menuHover';
import view from './view';
import { Chessground } from 'chessground';
import { init, attributesModule, eventListenersModule, classModule, propsModule } from 'snabbdom';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export function initModule(config: Editor.Config): LichessEditor {
  const ctrl = new EditorCtrl(config, redraw);

  const el = config.el || document.getElementById('board-editor')!;

  el.innerHTML = '';

  const inner = document.createElement('div');
  el.appendChild(inner);
  let vnode = patch(inner, view(ctrl));

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();

  return {
    getFen: ctrl.getFen.bind(ctrl),
    setOrientation: ctrl.setOrientation.bind(ctrl),
  };
}

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
