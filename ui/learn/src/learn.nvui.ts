import { type NvuiContext, makeContext } from 'lib/nvui/chess';

import type { LearnCtrl } from './ctrl';
import { renderNvui } from './view/nvuiView';

export type LearnNvuiContext = NvuiContext &
  Readonly<{
    ctrl: LearnCtrl;
  }>;

export interface NvuiPlugin {
  render(): import('snabbdom').VNode;
}

export function initModule(ctrl: LearnCtrl): NvuiPlugin {
  const ctx = makeContext<LearnNvuiContext>({ ctrl }, ctrl.redraw);
  return {
    render: () => renderNvui(ctx),
  };
}
