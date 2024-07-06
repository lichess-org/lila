import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, onInsert, bind, MaybeVNode, VNode } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../studyDeps';
import { tourSide, renderRelayTour } from './relayTourView';
import { renderVideoPlayer } from './videoPlayerView';
import {
  type RelayViewContext,
  viewContext,
  renderBoard,
  renderMain,
  renderControls,
  renderTools,
  renderUnderboard,
} from '../../view/components';
import RelayCtrl from './relayCtrl';

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

export function renderPinnedImage(ctx: RelayViewContext): MaybeVNode {
  const { allowVideo, relay } = ctx;
  if (!relay.pinStreamer() || !relay.data.pinned?.image) return undefined;
  return h('img.link', {
    attrs: { src: relay.data.pinned.image },
    hook: bind('click', () => {
      if (!allowVideo) {
        window.open(`${window.location.origin}/streamer/${relay.data.pinned!.userId}`, '_blank', 'noopener');
        return;
      }
      const url = new URL(location.href);
      url.searchParams.set('embed', relay.data.pinned!.userId);
      window.location.replace(url);
    }),
  });
}

export function allowVideo(): boolean {
  return window.getComputedStyle(document.body).getPropertyValue('---allow-video') === 'true';
}

function renderBoardView(ctx: RelayViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(ctx, renderEmbedPlaceholder(ctx)),
    renderControls(ctrl),
    renderUnderboard(ctx),
    tourSide(ctx),
    deps.relayManager(relay, study),
  ];
}

function renderEmbedPlaceholder(ctx: RelayViewContext): MaybeVNode {
  return ctx.relay.data.videoUrls
    ? renderVideoPlayer(ctx.relay)
    : ctx.relay.isShowingPinnedImage()
    ? renderPinnedImage(ctx)
    : undefined;
}
