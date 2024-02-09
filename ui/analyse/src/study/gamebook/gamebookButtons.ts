import { bind, dataIcon } from 'common/snabbdom';
import { VNode, h } from 'snabbdom';
import AnalyseCtrl from '../../ctrl';
import { StudyCtrl } from '../interfaces';

export function playButtons(root: AnalyseCtrl): VNode | undefined {
  const study = root.study!,
    ctrl = study.gamebookPlay();
  if (!ctrl) return;
  const state = ctrl.state,
    fb = state.feedback,
    myTurn = fb === 'play';
  return h(
    'div.study__buttons.gamebook-buttons',
    h('div.right', [
      h(
        'div.text.back',
        {
          attrs: { 'data-icon': 'I', disabled: !root.path },
          hook: bind('click', () => root.userJump(''), ctrl.redraw),
        },
        root.trans.noarg('back')
      ),
      h(
        'div.text.solution',
        {
          attrs: { 'data-icon': 'G', disabled: !myTurn },
          hook: bind('click', ctrl.solution, ctrl.redraw),
        },
        root.trans.noarg('viewTheSolution')
      ),
      overrideButton(study),
    ])
  );
}

export function overrideButton(study: StudyCtrl): VNode | undefined {
  if (study.data.chapter.gamebook) {
    const o = study.vm.gamebookOverride;
    if (study.members.canContribute())
      return h(
        'a.fbt.text.preview',
        {
          class: { active: o === 'play' },
          attrs: dataIcon('v'),
          hook: bind(
            'click',
            () => {
              study.setGamebookOverride(o === 'play' ? undefined : 'play');
            },
            study.redraw
          ),
        },
        study.trans.noarg('preview')
      );
    else {
      const isAnalyse = o === 'analyse',
        ctrl = study.gamebookPlay();
      if (isAnalyse || (ctrl && ctrl.state.feedback === 'end'))
        return h(
          'a.fbt.text.preview',
          {
            class: { active: isAnalyse },
            attrs: dataIcon('A'),
            hook: bind(
              'click',
              () => {
                study.setGamebookOverride(isAnalyse ? undefined : 'analyse');
              },
              study.redraw
            ),
          },
          study.trans.noarg('analyse')
        );
    }
  }
  return undefined;
}
