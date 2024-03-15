import { view as cevalView } from 'ceval';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../../study/studyDeps';
import { tourSide } from '../../study/relay/relayTourView';
import {
  type ViewContext,
  viewContext,
  renderBoard,
  renderMain,
  renderControls,
  renderTools,
  renderUnderboard,
} from '../../view/components';

export function relayView(ctrl: AnalyseCtrl, deps?: typeof studyDeps) {
  const ctx = viewContext(ctrl, deps);
  const { study, relay, tourUi } = ctx;

  const renderTourView = () => [tourUi, tourSide(ctrl, study!, relay!), deps?.relayManager(relay!, study!)];

  return renderMain(ctx, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    study && deps?.studyView.overboard(study),
    ...(tourUi ? renderTourView() : renderBoardView(ctx)),
  ]);
}

function renderBoardView(ctx: ViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(ctx),
    renderControls(ctrl),
    renderUnderboard(ctx),
    tourSide(ctrl, study!, relay!),
    deps?.relayManager(relay!, study!),
  ];
}
