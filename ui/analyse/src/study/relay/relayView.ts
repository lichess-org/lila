import { view as cevalView } from 'lib/ceval';
import { displayColumns, isTouchDevice } from 'lib/device';
import * as licon from 'lib/licon';
import { bind, dataIcon, hl, type VNode } from 'lib/view';

import type AnalyseCtrl from '@/ctrl';
import { view as keyboardView } from '@/keyboard';
import {
  type RelayViewContext,
  viewContext,
  renderBoard,
  renderMain,
  renderUnderboard,
} from '@/view/components';
import { renderControls } from '@/view/controls';
import { renderTools } from '@/view/tools';

import type * as studyDeps from '../studyDeps';
import type RelayCtrl from './relayCtrl';
import { tourSide, renderRelayTour } from './relayTourView';
import { allowVideo } from './videoPlayer';

export function relayView(
  ctrl: AnalyseCtrl,
  study: studyDeps.StudyCtrl,
  relay: RelayCtrl,
  deps: typeof studyDeps,
): VNode {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay, allowVideo: allowVideo() };

  const renderTourView = () => {
    const resizable = displayColumns() > (ctx.hasRelayTour ? 1 : 2);

    return [
      renderRelayTour(ctx),
      tourSide(ctx, resizable && deps.relayManager(relay, study)),
      !resizable && deps.relayManager(relay, study),
    ];
  };

  return renderMain(
    ctx,
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ctx.hasRelayTour ? renderTourView() : renderBoardView(ctx),
  );
}

export const backToLiveView = (ctrl: AnalyseCtrl) =>
  ctrl.study?.isRelayAwayFromLive()
    ? hl(
        'button.fbt.relay-back-to-live.text',
        {
          attrs: dataIcon(licon.PlayTriangle),
          hook: bind(
            'click',
            () => {
              const p = ctrl.study?.data.chapter.relayPath;
              if (p) ctrl.userJump(p);
            },
            ctrl.redraw,
          ),
        },
        i18n.broadcast.backToLiveMove,
      )
    : undefined;

function renderBoardView(ctx: RelayViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  const resizable = !isTouchDevice() && displayColumns() > 2;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(ctx, relay.userClosedTheVideoEmbed() ? undefined : relay.videoPlayer?.render()),
    renderControls(ctrl),
    !ctrl.isEmbed && renderUnderboard(ctx),
    tourSide(ctx, resizable && deps.relayManager(relay, study)),
    !resizable && deps.relayManager(relay, study),
  ];
}
