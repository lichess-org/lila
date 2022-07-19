import { AnalyseApi, AnalyseOpts } from './interfaces';

import makeCtrl from './ctrl';
import view from './view';
import boot from './boot';
import { Shogiground } from 'shogiground';
import LishogiChat from 'chat';

import { init, classModule, attributesModule } from 'snabbdom';
import menuHover from 'common/menuHover';

export const patch = init([classModule, attributesModule]);

export function start(opts: AnalyseOpts): AnalyseApi {
  opts.element = document.querySelector('main.analyse') as HTMLElement;

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

// that's for the rest of lishogi to access shogiground
// without having to include it a second time
window.Shogiground = Shogiground;
window.LishogiChat = LishogiChat;
