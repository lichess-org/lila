import { type NvuiContext, makeContext } from 'lib/nvui/chess';
import type PuzzleCtrl from './ctrl';
import { renderNvui } from './view/nvuiView';

export type PuzzleNvuiContext = NvuiContext &
  Readonly<{
    ctrl: PuzzleCtrl;
  }>;
export function initModule(ctrl: PuzzleCtrl) {
  const ctx = makeContext<PuzzleNvuiContext>({ ctrl });
  return {
    render: () => renderNvui(ctx),
  };
}
