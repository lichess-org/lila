import type { VNode } from 'snabbdom';

import menuHover from 'lib/menuHover';
import { mountApp } from 'lib/view';

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

    const view = makeView(deps);
    const ctrl = (site.analysis = mountApp({
      el: opts.element,
      patch,
      makeCtrl: redraw => {
        const state: { controller?: InstanceType<typeof makeCtrl> } = {};
        const ctrlRedraw = () => {
          const controller = state.controller;
          if (!controller) return;

          const study = controller.study;
          if (study && !study.chapters.list.get(study.vm.chapterId)) return;

          redraw();
        };
        const controller = new makeCtrl(opts, ctrlRedraw, deps?.StudyCtrl);
        state.controller = controller;

        return controller;
      },
      view,
    }));

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
