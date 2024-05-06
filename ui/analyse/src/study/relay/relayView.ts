import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, onInsert, bind, MaybeVNode, VNode } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../studyDeps';
import { tourSide, renderRelayTour } from './relayTourView';
import { renderVideoPlayer, player } from './videoPlayerView';
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
  const { wide, tinyBoard } = queryBody();
  return renderMain(ctx, { wide: wide, 'with-video': !!relay.data.videoUrls, 'tiny-board': tinyBoard }, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ...(ctx.hasRelayTour
      ? [renderRelayTour(ctx), ...tourSide(ctx), deps.relayManager(relay, study)]
      : renderBoardView(ctx, wide)),
  ]);
}

export function addResizeListener(redraw: () => void) {
  let [oldWide, oldShowVideo, oldTinyBoard] = [false, false, false];
  window.addEventListener(
    'resize',
    () => {
      const { wide, allowVideo, tinyBoard } = queryBody(),
        placeholder = document.getElementById('video-player-placeholder') ?? undefined,
        showVideo = allowVideo && !!placeholder;
      player?.cover(allowVideo ? placeholder : undefined);
      if (oldShowVideo !== showVideo || oldWide !== wide || oldTinyBoard !== tinyBoard) redraw();
      [oldWide, oldShowVideo, oldTinyBoard] = [wide, showVideo, tinyBoard];
    },
    { passive: true },
  );
}

function queryBody() {
  const docStyle = window.getComputedStyle(document.body),
    scale = (parseFloat(docStyle.getPropertyValue('--zoom')) / 100) * 0.75 + 0.25,
    boardWidth = scale * window.innerHeight,
    allowVideo = docStyle.getPropertyValue('--allow-video') === 'true',
    leftSidePlusGaps = 410,
    minWidthForTwoColumns = 500;
  // zoom -> scale calc from file://./../../../../common/css/layout/_uniboard.scss
  // leftSidePlusGaps and minWidthForTwoColumns aren't exact and don't have to be
  return {
    wide: window.innerWidth - leftSidePlusGaps - boardWidth > minWidthForTwoColumns,
    allowVideo,
    tinyBoard: scale <= 0.67,
  };
}

function renderBoardView(ctx: RelayViewContext, wide: boolean) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;

  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    wide && renderEmbedPlaceholder(ctx),
    renderTools(ctx, wide ? undefined : renderEmbedPlaceholder(ctx)),
    renderControls(ctrl),
    renderUnderboard(ctx),
    ...tourSide(ctx),
    deps.relayManager(relay, study),
  ];
}

export function renderStreamerMenu(relay: RelayCtrl) {
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

function renderEmbedPlaceholder(ctx: RelayViewContext): MaybeVNode {
  return ctx.relay.data.videoUrls
    ? renderVideoPlayer(ctx.relay)
    : ctx.relay.pinStreamer() && ctx.relay.allowPinnedImageOnUniboards()
    ? renderPinnedImage(ctx)
    : undefined;
}
