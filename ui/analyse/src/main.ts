import { AnalyseApi, AnalyseOpts } from './interfaces';
import AnalyseCtrl from './ctrl';

import makeCtrl from './ctrl';
import view from './view';
import boot from './boot';
import { Chessground } from 'chessground';
import * as chat from 'chat';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import { menuHover } from 'common/menuHover';

menuHover();

export const patch = init([klass, attributes]);

export function start(opts: AnalyseOpts): AnalyseApi {

  opts.element = document.querySelector('main.analyse') as HTMLElement;

  let vnode: VNode, ctrl: AnalyseCtrl;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  return {
    socketReceive: ctrl.socket.receive,
    path: () => ctrl.path,
    setChapter(id: string) {
      if (ctrl.study) ctrl.study.setChapter(id);
    }
  }
}

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;
window.LichessChat = chat;
