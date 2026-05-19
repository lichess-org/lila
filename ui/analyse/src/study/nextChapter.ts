import { h } from 'snabbdom';

import * as licon from 'lib/licon';
import { ops as treeOps } from 'lib/tree/tree';
import { bind } from 'lib/view';

import type AnalyseCtrl from '../ctrl';

export const renderNextChapter = (ctrl: AnalyseCtrl) =>
  !ctrl.opts.relay && ctrl.study?.hasNextChapter()
    ? h(
        'button.next.text',
        {
          attrs: { 'data-icon': licon.PlayTriangle, type: 'button' },
          hook: bind('click', ctrl.study.goToNextChapter),
          class: { highlighted: !!ctrl.node.outcome() || ctrl.node === treeOps.last(ctrl.mainline) },
        },
        i18n.study.nextChapter,
      )
    : null;
