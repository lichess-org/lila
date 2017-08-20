import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, dataIcon } from '../../util';
import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';
import { shareButton } from '../studyView';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
  ctrl = study.gamebookPlay();
  if (!ctrl) return;
  const state = ctrl.state,
  fb = state.feedback,
  myTurn = fb === 'play';
  return h('div.study_buttons', [
    shareButton(study),
    h('div.gb_buttons', [
      myTurn && state.hint ? h('a.button.text.hint', {
        attrs: dataIcon(''),
        hook: bind('click', ctrl.hint, ctrl.redraw)
      }, 'Get a hint') : null,
      myTurn ? h('a.button.text.solution', {
        attrs: dataIcon('G'),
        hook: bind('click', ctrl.solution, ctrl.redraw)
      }, 'View the solution') : undefined,
      study.vm.gamebookOverride === 'play' ? previewButton(study) : undefined
    ]),
  ]);
}

export function previewButton(study: StudyCtrl): VNode | undefined {
  if (study.data.chapter.gamebook) return h('a.button.text.preview', {
    class: { active: study.vm.gamebookOverride === 'play' },
    attrs: dataIcon('v'),
    hook: bind('click', () => {
      study.setGamebookOverride(study.vm.gamebookOverride === 'play' ? undefined : 'play');
    }, study.redraw)
  }, 'Preview');
}
