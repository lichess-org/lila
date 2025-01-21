import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { bind, dataIcon, looseH as h, onInsert, type VNode } from 'common/snabbdom';
import * as licon from 'common/licon';
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
import type RelayCtrl from './relayCtrl';

export function relayView(
  ctrl: AnalyseCtrl,
  study: studyDeps.StudyCtrl,
  relay: RelayCtrl,
  deps: typeof studyDeps,
): VNode {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay, allowVideo: allowVideo() };

  const renderTourView = () => [renderRelayTour(ctx), tourSide(ctx), deps.relayManager(relay, study)];

  return renderMain(ctx, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ...(ctx.hasRelayTour ? renderTourView() : renderBoardView(ctx)),
  ]);
}

export const backToLiveView = (ctrl: AnalyseCtrl) =>
  ctrl.study?.isRelayAwayFromLive()
    ? h(
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
  return h(
    'div.streamer-menu-anchor',
    h(
      'div.streamer-menu',
      {
        hook: onInsert(
          onClickAway(() => {
            relay.showStreamerMenu(false);
            relay.redraw();
          }),
        ),
      },
      relay.streams.map(([id, name]) =>
        h('a.streamer.text', { attrs: { 'data-icon': licon.Mic, href: makeUrl(id) } }, name),
      ),
    ),
  );
}

export function allowVideo(): boolean {
  return window.getComputedStyle(document.body).getPropertyValue('---allow-video') === 'true';
}

function renderBoardView(ctx: RelayViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(ctx, relay.noEmbed() ? undefined : relay.videoPlayer?.render()),
    renderControls(ctrl),
    !ctrl.isEmbed && renderUnderboard(ctx),
    tourSide(ctx),
    deps.relayManager(relay, study),
  ];
}
