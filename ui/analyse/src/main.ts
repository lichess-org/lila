import { attributesModule, classModule, init } from 'snabbdom';
import boot from './boot';
import LichessChat from 'chat';
// eslint-disable-next-line no-duplicate-imports
import makeCtrl from './ctrl';
import menuHover from 'common/menuHover';
import view from './view';
import { AnalyseApi, AnalyseOpts } from './interfaces';
import { Chessground } from 'chessground';

export const patch = init([classModule, attributesModule]);

export function start(opts: AnalyseOpts): AnalyseApi {
  opts.element = document.querySelector('main.analyse') as HTMLElement;
  opts.trans = lichess.trans(opts.i18n);

  const ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  let vnode = patch(opts.element, blueprint);

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  menuHover();

  return {
    socketReceive: ctrl.socket.receive,
    path: () => ctrl.path,
    setChapter(id: string) {
      if (ctrl.study) ctrl.study.setChapter(id);
    },
  };
}

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = LichessChat;
