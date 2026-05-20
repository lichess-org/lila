import { type Prop, prop } from 'lib';
import { type NvuiContext, makeContext } from 'lib/nvui/chess';

import type AnalyseCtrl from './ctrl';
import type { NvuiPlugin } from './interfaces';
import type * as studyDeps from './study/studyDeps';
import { renderNvui, initNvui } from './view/nvuiView';

export type AnalyseNvuiContext = NvuiContext &
  Readonly<{
    ctrl: AnalyseCtrl;
    deps?: typeof studyDeps;
    analysisInProgress: Prop<boolean>;
  }>;

export function initModule(ctrl: AnalyseCtrl): NvuiPlugin {
  const ctx = makeContext<AnalyseNvuiContext>(
    {
      ctrl,
      analysisInProgress: prop(false),
    },
    ctrl.redraw,
  );
  initNvui(ctx);
  return { render: deps => renderNvui({ ...ctx, deps }) };
}
