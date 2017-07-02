/// <reference types="types/lichess-jquery" />

import { AnalyseOpts } from './interfaces';
import AnalyseController from './ctrl';

import makeCtrl from './ctrl';
import view from './view';
import { main as studyView } from './study/studyView';
import { main as studyPracticeView } from './study/practice/studyPracticeView';
import boot = require('./boot');
import { Chessground } from 'chessground';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

export const patch = init([klass, attributes]);

export function start(opts: AnalyseOpts) {

  let vnode: VNode, ctrl: AnalyseController;

  let redrawSide = () => {};

  function redraw() {
    vnode = patch(vnode, view(ctrl));
    redrawSide();
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  if (ctrl.study && opts.sideElement) {
    const sideView = ctrl.studyPractice ? studyPracticeView : studyView;
    let sideVnode = patch(opts.sideElement, sideView(ctrl.study));
    redrawSide = () => {
      sideVnode = patch(sideVnode, sideView(ctrl.study));
    }
  }

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
