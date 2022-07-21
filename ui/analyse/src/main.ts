import { Shogiground } from 'shogiground';
import LishogiChat from 'chat';

import { AnalyseApi, AnalyseOpts } from './interfaces';
import patch from './patch';
import makeCtrl from './ctrl';
import makeView from './view';
import makeBoot from './boot';

import menuHover from 'common/menuHover';

export { patch };

export function start(opts: AnalyseOpts): AnalyseApi {
  opts.element = document.querySelector('main.analyse') as HTMLElement;

  const ctrl = new makeCtrl(opts, redraw);

  const blueprint = makeView(ctrl);
  opts.element.innerHTML = '';
  let vnode = patch(opts.element, blueprint);

  function redraw() {
    vnode = patch(vnode, makeView(ctrl));
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

export const boot = makeBoot(start);

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
window.LishogiChat = LishogiChat;
