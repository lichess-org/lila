import type AnalyseCtrl from './ctrl';
import type { NvuiPlugin } from './interfaces';
import * as nvui from 'lib/nvui/chess';
import type * as studyDeps from './study/studyDeps';
import { renderNvui, initNvui } from './view/nvuiView';
import { Notify } from 'lib/nvui/notify';
import { type Prop, prop } from 'lib';

export type NvuiContext = Readonly<{
  ctrl: AnalyseCtrl;
  deps?: typeof studyDeps;
  analysisInProgress: Prop<boolean>;
  notify: Notify;
  moveStyle: ReturnType<typeof nvui.styleSetting>;
  pieceStyle: ReturnType<typeof nvui.pieceSetting>;
  prefixStyle: ReturnType<typeof nvui.prefixSetting>;
  positionStyle: ReturnType<typeof nvui.positionSetting>;
  boardStyle: ReturnType<typeof nvui.boardSetting>;
}>;

export function initModule(ctrl: AnalyseCtrl): NvuiPlugin {
  const ctx: NvuiContext = {
    ctrl,
    notify: new Notify(),
    analysisInProgress: prop(false),
    moveStyle: nvui.styleSetting(),
    pieceStyle: nvui.pieceSetting(),
    prefixStyle: nvui.prefixSetting(),
    positionStyle: nvui.positionSetting(),
    boardStyle: nvui.boardSetting(),
  };
  initNvui(ctx);
  return { render: deps => renderNvui({ ...ctx, deps }) };
}
