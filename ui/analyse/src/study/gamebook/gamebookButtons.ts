import { bind, snabIcon, type VNode, hl } from 'lib/view';

import type AnalyseCtrl from '@/ctrl';

import type StudyCtrl from '../studyCtrl';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
    ctrl = study.gamebookPlay;
  if (!ctrl) return undefined;
  const state = ctrl.state,
    fb = state.feedback,
    myTurn = fb === 'play';
  return hl('div.gamebook-buttons', [
    root.path &&
      hl(
        'button.fbt.text.back',
        {
          attrs: { type: 'button' },
          hook: bind('click', () => root.userJump(''), ctrl.redraw),
        },
        [snabIcon('lessThan'), i18n.study.back],
      ),
    myTurn &&
      hl(
        'button.fbt.text.solution',
        {
          attrs: { type: 'button' },
          hook: bind('click', ctrl.solution, ctrl.redraw),
        },
        [snabIcon('playTriangle'), i18n.site.viewTheSolution],
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
          attrs: { type: 'button' },
          hook: bind(
            'click',
            () => study.setGamebookOverride(o === 'play' ? undefined : 'play'),
            study.redraw,
          ),
        },
        [snabIcon('eye'), 'Preview'],
      );
    else {
      const isAnalyse = o === 'analyse',
        ctrl = study.gamebookPlay;
      if (isAnalyse || ctrl?.state.feedback === 'end')
        return hl(
          'a.fbt.text.preview',
          {
            class: { active: isAnalyse },
            hook: bind(
              'click',
              () => study.setGamebookOverride(isAnalyse ? undefined : 'analyse'),
              study.redraw,
            ),
          },
          [snabIcon('microscope'), i18n.site.analysis],
        );
    }
  }
  return undefined;
}
