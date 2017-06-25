/// <reference types="types/lichess-jquery" />

import { AnalyseOpts } from './interfaces';
import AnalyseController from './ctrl';

import makeCtrl from './ctrl';
import view from './view';
// import studyView = require('./study/studyView');
// import studyPracticeView = require('./study/practice/studyPracticeView');
import boot = require('./boot');
import { Chessground } from 'chessground';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

export const patch = init([klass, attributes]);

export function start(opts: AnalyseOpts) {

  let vnode: VNode, ctrl: AnalyseController;

  function redraw() {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  // if (controller.study && opts.sideElement) m.module(opts.sideElement, {
  //   controller: function() {
  //     m.redraw.strategy("diff"); // prevents double full redraw on page load
  //     return controller.study;
  //   },
  //   view: controller.studyPractice ? studyPracticeView.main : studyView.main
  // });

  return {
    socketReceive: ctrl.socket.receive,
    jumpToIndex(index: number): void {
      ctrl.jumpToIndex(index);
      redraw();
    },
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
