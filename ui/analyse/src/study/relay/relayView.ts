import { view as cevalView } from 'lib/ceval/ceval';
import { onClickAway } from 'lib';
import { bind, dataIcon, hl, onInsert, type VNode } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import type AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../studyDeps';
import { tourSide, renderRelayTour } from './relayTourView';
import {
  type RelayViewContext,
  viewContext,
  renderBoard,
  renderMain,
  renderControls,
  renderTools,
  renderUnderboard,
} from '../../view/components';
import { displayColumns, isTouchDevice } from 'lib/device';
import type RelayCtrl from './relayCtrl';

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

export function renderStreamerMenu(relay: RelayCtrl): VNode {
  const makeUrl = (id: string) => {
    const url = new URL(location.href);
    url.searchParams.set('embed', id);
    return url.toString();
  };
  return hl(
    'div.streamer-menu-anchor',
    hl(
      'div.streamer-menu',
      {
        hook: onInsert(
          onClickAway(() => {
            relay.showStreamerMenu(false);
            relay.redraw();
          }),
        ),
      },
      relay.streams.map(([id, info]) =>
        hl('a.streamer.text', { attrs: { 'data-icon': licon.Mic, href: makeUrl(id) } }, [
          info.name,
          hl('i', info.lang),
        ]),
      ),
    ),
  );
}

export function allowVideo(): boolean {
  return window.getComputedStyle(document.body).getPropertyValue('---allow-video') === 'true';
}

function renderBoardView(ctx: RelayViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  const resizable = !isTouchDevice() && displayColumns() > 2;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(ctx, relay.noEmbed() ? undefined : relay.videoPlayer?.render()),
    renderControls(ctrl),
    !ctrl.isEmbed && renderUnderboard(ctx),
    tourSide(ctx, resizable && deps.relayManager(relay, study)),
    !resizable && deps.relayManager(relay, study),
  ];
}
