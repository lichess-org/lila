import { VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind, dataIcon, looseH as h } from 'common/snabbdom';
import AnalyseCtrl from '../../ctrl';
import StudyCtrl from '../studyCtrl';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
    ctrl = study.gamebookPlay;
  if (!ctrl) return;
  const state = ctrl.state,
    fb = state.feedback,
    myTurn = fb === 'play';
  return h('div.gamebook-buttons', [
    root.path &&
      h(
        'button.fbt.text.back',
        {
          attrs: { 'data-icon': licon.LessThan, type: 'button' },
          hook: bind('click', () => root.userJump(''), ctrl.redraw),
        },
        root.trans.noarg('back'),
      ),
    myTurn &&
      h(
        'button.fbt.text.solution',
        {
          attrs: { 'data-icon': licon.PlayTriangle, type: 'button' },
          hook: bind('click', ctrl.solution, ctrl.redraw),
        },
        root.trans.noarg('viewTheSolution'),
      ),
    overrideButton(study),
  ]);
}

export function overrideButton(study: StudyCtrl): VNode | undefined {
  if (study.data.chapter.gamebook) {
    const o = study.vm.gamebookOverride;
    if (study.members.canContribute())
      return h(
        'button.fbt.text.preview',
        {
          class: { active: o === 'play' },
          attrs: { 'data-icon': licon.Eye, type: 'button' },
          hook: bind(
            'click',
            () => study.setGamebookOverride(o === 'play' ? undefined : 'play'),
            study.redraw,
          ),
        },
        'Preview',
      );
    else {
      const isAnalyse = o === 'analyse',
        ctrl = study.gamebookPlay;
      if (isAnalyse || (ctrl && ctrl.state.feedback === 'end'))
        return h(
          'a.fbt.text.preview',
          {
            class: { active: isAnalyse },
            attrs: dataIcon(licon.Microscope),
            hook: bind(
              'click',
              () => study.setGamebookOverride(isAnalyse ? undefined : 'analyse'),
              study.redraw,
            ),
          },
          study.trans.noarg('analysis'),
        );
    }
  }
  return undefined;
}
