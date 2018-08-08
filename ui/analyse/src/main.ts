import { AnalyseOpts } from './interfaces';
import AnalyseCtrl from './ctrl';

import makeCtrl from './ctrl';
import view from './view';
import { main as studyView } from './study/studyView';
import { main as studyPracticeView } from './study/practice/studyPracticeView';
import { StudyCtrl } from './study/interfaces';
import boot = require('./boot');
import { Draughtsground } from 'draughtsground';
import * as chat from 'chat';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode'
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';

export const patch = init([klass, attributes]);

export function start(opts: AnalyseOpts) {

  let vnode: VNode, ctrl: AnalyseCtrl;

  let redrawSide = () => {};

  function redraw() {
    vnode = patch(vnode, view(ctrl));
    redrawSide();
  }

  ctrl = new makeCtrl(opts, redraw);

  const blueprint = view(ctrl);
  opts.element.innerHTML = '';
  vnode = patch(opts.element, blueprint);

  const study: StudyCtrl | undefined = ctrl.study;

  if (study && opts.sideElement) {
    const sideView = ctrl.studyPractice ? studyPracticeView : studyView;
    let sideVnode = patch(opts.sideElement, sideView(study));
    redrawSide = () => {
      sideVnode = patch(sideVnode, sideView(study));
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

// that's for the rest of lidraughts to access draughtsground
// without having to include it a second time
window.Draughtsground = Draughtsground;
window.LidraughtsChat = chat;
