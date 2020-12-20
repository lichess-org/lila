import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, dataIcon } from '../../util';
import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
    ctrl = study.gamebookPlay();
  if (!ctrl) return;
  const state = ctrl.state,
    fb = state.feedback,
    myTurn = fb === 'play';
  return h('div.gamebook-buttons', [
    root.path ? h('a.fbt.text.back', {
      attrs: dataIcon('I'),
      hook: bind('click', () => root.userJump(''), ctrl.redraw)
    }, 'Back') : null,
    myTurn ? h('a.fbt.text.solution', {
      attrs: dataIcon('G'),
      hook: bind('click', ctrl.solution, ctrl.redraw)
    }, 'View the solution') : undefined,
    overrideButton(study)
  ]);
}

export function overrideButton(study: StudyCtrl): VNode | undefined {
  if (study.data.chapter.gamebook) {
    const o = study.vm.gamebookOverride;
    if (study.members.canContribute()) return h('a.fbt.text.preview', {
      class: { active: o === 'play' },
      attrs: dataIcon('v'),
      hook: bind('click', () => {
        study.setGamebookOverride(o === 'play' ? undefined : 'play');
      }, study.redraw)
    }, 'Preview');
    else {
      const isAnalyse = o === 'analyse',
        ctrl = study.gamebookPlay();
      if (isAnalyse || (ctrl && ctrl.state.feedback === 'end')) return h('a.fbt.text.preview', {
        class: { active: isAnalyse },
        attrs: dataIcon('A'),
        hook: bind('click', () => {
          study.setGamebookOverride(isAnalyse ? undefined : 'analyse');
        }, study.redraw)
      }, 'Analyse');
    }
  }
  return undefined;
}
