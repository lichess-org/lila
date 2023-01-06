import { h } from 'snabbdom';
import { bind } from 'common/snabbdom';
import AnalyseCtrl from '../ctrl';
import { ops as treeOps } from 'tree';

export const renderNextChapter = (ctrl: AnalyseCtrl) =>
  !ctrl.embed && !ctrl.opts.relay && ctrl.study?.hasNextChapter()
    ? h(
        'button.next.text',
        {
          attrs: {
            'data-icon': '',
            type: 'button',
          },
          hook: bind('click', ctrl.study.goToNextChapter),
          class: {
            highlighted: !!ctrl.outcome() || ctrl.node == treeOps.last(ctrl.mainline),
          },
        },
        ctrl.trans.noarg('nextChapter')
      )
    : null;
