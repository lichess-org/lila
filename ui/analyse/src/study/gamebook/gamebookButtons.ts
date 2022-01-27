import { h, VNode } from 'snabbdom';
import { bind, dataIcon } from 'common/snabbdom';
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
    root.path
      ? h(
          'button.fbt.text.back',
          {
            attrs: {
              'data-icon': '',
              type: 'button',
            },
            hook: bind('click', () => root.userJump(''), ctrl.redraw),
          },
          root.trans.noarg('back')
        )
      : null,
    myTurn
      ? h(
          'button.fbt.text.solution',
          {
            attrs: {
              'data-icon': '',
              type: 'button',
            },
            hook: bind('click', ctrl.solution, ctrl.redraw),
          },
          root.trans.noarg('viewTheSolution')
        )
      : undefined,
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
          attrs: {
            'data-icon': '',
            type: 'button',
          },
          hook: bind(
            'click',
            () => {
              study.setGamebookOverride(o === 'play' ? undefined : 'play');
            },
            study.redraw
          ),
        },
        'Preview'
      );
    else {
      const isAnalyse = o === 'analyse',
        ctrl = study.gamebookPlay();
      if (isAnalyse || (ctrl && ctrl.state.feedback === 'end'))
        return h(
          'a.fbt.text.preview',
          {
            class: { active: isAnalyse },
            attrs: dataIcon(''),
            hook: bind(
              'click',
              () => {
                study.setGamebookOverride(isAnalyse ? undefined : 'analyse');
              },
              study.redraw
            ),
          },
          study.trans.noarg('analysis')
        );
    }
  }
  return undefined;
}
