import AnalyseCtrl from './ctrl';
import attributes from 'snabbdom/modules/attributes';
import boot from './boot';
import klass from 'snabbdom/modules/class';
import LichessChat from 'chat';
import makeCtrl from './ctrl';
import menuHover from 'common/menuHover';
import view from './view';
import { AnalyseApi, AnalyseOpts } from './interfaces';
import { Chessground } from 'chessground';
import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';

export const patch = init([klass, attributes]);

export function start(opts: AnalyseOpts): AnalyseApi {
  opts.element = document.querySelector('main.analyse') as HTMLElement;
  opts.trans = lichess.trans(opts.i18n);

  let vnode: VNode, ctrl: AnalyseCtrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

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
