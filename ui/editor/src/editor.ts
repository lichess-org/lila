import { init, attributesModule, eventListenersModule, classModule, propsModule } from 'snabbdom';

import menuHover from 'lib/menuHover';

import EditorCtrl from './ctrl';
import type { LichessEditor, Config } from './interfaces';
import view, { loadFromImage } from './view';

const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export type { LichessEditor } from './interfaces';

let pasteListener: (e: ClipboardEvent) => void;

export function initModule(config: Config): LichessEditor {
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

  if (pasteListener) {
    document.removeEventListener('paste', pasteListener);
  }
  pasteListener = e => {
    const file = Array.from(e.clipboardData?.files || []).find(file => file.type.startsWith('image/'));
    if (file) {
      e.preventDefault();
      void loadFromImage(ctrl, file);
    }
  };
  document.addEventListener('paste', pasteListener);

  return {
    getFen: ctrl.getFen.bind(ctrl),
    setFen: fen => ctrl.setFen(fen),
    setOrientation: ctrl.setOrientation.bind(ctrl),
    setVariant: ctrl.setVariant.bind(ctrl),
  };
}
