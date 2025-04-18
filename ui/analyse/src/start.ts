import makeCtrl from './ctrl';
import menuHover from 'lib/menuHover';
import makeView from './view/main';
import type { AnalyseApi, AnalyseOpts } from './interfaces';
import type { VNode } from 'snabbdom';
import type * as studyDeps from './study/studyDeps';

export default function (
  patch: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode,
  deps?: typeof studyDeps,
) {
  return function (opts: AnalyseOpts): AnalyseApi {
    opts.element = document.querySelector('main.analyse') as HTMLElement;

    const ctrl = (site.analysis = new makeCtrl(opts, redraw, deps?.StudyCtrl));
    const view = makeView(deps);

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
  };
}
