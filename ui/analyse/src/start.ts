import type { VNode } from 'snabbdom';

import type { CevalCtrl } from 'lib/ceval';
import menuHover from 'lib/menuHover';

import makeCtrl from './ctrl';
import type { AnalyseApi, AnalyseOpts } from './interfaces';
import type * as studyDeps from './study/studyDeps';
import makeView from './view/main';

export default function (
  patch: (oldVnode: VNode | Element | DocumentFragment, vnode: VNode) => VNode,
  deps?: typeof studyDeps,
) {
  return function (opts: AnalyseOpts): AnalyseApi {
    opts.element = document.querySelector('main.analyse') as HTMLElement;

    function mount(cfg: AnalyseOpts, previous: VNode | Element, ceval?: CevalCtrl) {
      let vnode: VNode | undefined;
      const ctrl = (site.analysis = new makeCtrl(cfg, redraw, deps?.StudyCtrl, ceval));
      const render = makeView(ctrl, deps);
      const view = () => Object.assign(render(), { key: ctrl.contextId });
      function redraw() {
        if (!vnode || ctrl.reaper.disposed) return;
        vnode = patch(vnode, view());
        ctrl.element = ctrl.opts.element = vnode.elm as HTMLElement;
      }
      vnode = patch(previous, view());
      ctrl.element = ctrl.opts.element = vnode.elm as HTMLElement;
      return {
        ctrl,
        get vnode() {
          return vnode!;
        },
      };
    }

    opts.element.innerHTML = '';
    let current = mount(opts, opts.element);
    menuHover();

    return {
      socketReceive: (type, data) => current.ctrl.socket.receive(type, data),
      path: () => current.ctrl.path,
      setChapter: id => current.ctrl.study?.setChapter(id) ?? Promise.resolve(false),
      restoreStudyPosition: request => current.ctrl.restoreStudyPosition(request),
      reload: cfg => {
        cfg.element = current.ctrl.element;
        const ceval = current.ctrl.ceval; // ceval workers can be expensive to recreate, so swap it over
        current.ctrl.reaper.dispose();
        current = mount(cfg, current.vnode, ceval);
        return current.ctrl.ready;
      },
    };
  };
}
