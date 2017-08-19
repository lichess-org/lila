import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { bind, dataIcon } from '../../util';
import AnalyseCtrl from '../../ctrl';
import { shareButton } from '../studyView';

export default function(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
  ctrl = study.gamebookPlay();
  if (!ctrl) return;
  const state = ctrl.state,
  fb = state.feedback;
  return h('div.study_buttons', [
    shareButton(study),
    fb === 'play' ? h('div.gb_buttons', [
      state.hint ? h('a.button.text', {
        attrs: dataIcon(''),
        hook: bind('click', ctrl.hint, ctrl.redraw)
      }, 'Get a hint') : null,
      h('a.button.text', {
        attrs: dataIcon('G'),
        hook: bind('click', ctrl.solution, ctrl.redraw)
      }, 'View the solution')
    ]) : undefined
  ]);
}
