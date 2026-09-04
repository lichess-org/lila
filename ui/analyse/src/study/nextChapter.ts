import { h } from 'snabbdom';

import { ops as treeOps } from 'lib/tree/tree';
import { bind, snabIcon } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

export const renderNextChapter = (ctrl: AnalyseCtrl) =>
  !ctrl.opts.relay && ctrl.study?.hasNextChapter()
    ? h(
        'button.next.text',
        {
          attrs: { type: 'button' },
          hook: bind('click', ctrl.study.goToNextChapter),
          class: { highlighted: !!ctrl.node.outcome() || ctrl.node === treeOps.last(ctrl.mainline) },
        },
        [snabIcon('playTriangle'), i18n.study.nextChapter],
      )
    : null;
