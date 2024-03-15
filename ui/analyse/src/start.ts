import makeCtrl from './ctrl';
import menuHover from 'common/menuHover';
import makeView from './view/main';
import { AnalyseApi, AnalyseOpts } from './interfaces';
import { VNode } from 'snabbdom';
import type * as studyDeps from './study/studyDeps';

export default function (
  patch: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode,
  deps?: typeof studyDeps,
) {
  return function (opts: AnalyseOpts): AnalyseApi {
    opts.element = document.querySelector('main.analyse') as HTMLElement;
    opts.trans = site.trans(opts.i18n);

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
