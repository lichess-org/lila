import * as licon from 'lib/licon';
import { bind, dataIcon, type VNode, hl } from 'lib/snabbdom';
import type AnalyseCtrl from '../../ctrl';
import type StudyCtrl from '../studyCtrl';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
    ctrl = study.gamebookPlay;
  if (!ctrl) return;
  const state = ctrl.state,
    fb = state.feedback,
    myTurn = fb === 'play';
  return hl('div.gamebook-buttons', [
    root.path &&
      hl(
        'button.fbt.text.back',
        {
          attrs: { 'data-icon': licon.LessThan, type: 'button' },
          hook: bind('click', () => root.userJump(''), ctrl.redraw),
        },
        i18n.study.back,
      ),
    myTurn &&
      hl(
        'button.fbt.text.solution',
        {
          attrs: { 'data-icon': licon.PlayTriangle, type: 'button' },
          hook: bind('click', ctrl.solution, ctrl.redraw),
        },
        i18n.site.viewTheSolution,
      ),
    overrideButton(study),
  ]);
}

export function overrideButton(study: StudyCtrl): VNode | undefined {
  if (study.data.chapter.gamebook) {
    const o = study.vm.gamebookOverride;
    if (study.members.canContribute())
      return hl(
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
        return hl(
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
          i18n.site.analysis,
        );
    }
  }
  return undefined;
}
