import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, onInsert, bind } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../../study/studyDeps';
import { tourSide } from '../../study/relay/relayTourView';
import { renderVideoPlayer, player } from './videoPlayerView';
import RelayCtrl from './relayCtrl';
import {
  type RelayViewContext,
  viewContext,
  renderBoard,
  renderMain,
  renderControls,
  renderTools,
  renderUnderboard,
} from '../../view/components';

export function relayView(
  ctrl: AnalyseCtrl,
  study: studyDeps.StudyCtrl,
  relay: RelayCtrl,
  deps: typeof studyDeps,
) {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay },
    { wide, tinyBoard } = queryBody();
  return renderMain(ctx, { wide: wide, 'with-video': !!relay.data.videoUrls, 'tiny-board': tinyBoard }, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ...(ctx.tourUi
      ? [ctx.tourUi, ...tourSide(ctrl, study, relay), deps.relayManager(relay, study)]
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
    wide && renderImageOrPlayer(relay),
    renderTools(ctx, wide ? undefined : renderImageOrPlayer(relay)),
    renderControls(ctrl),
    renderUnderboard(ctx),
    ...tourSide(ctrl, study, relay),
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

function renderImageOrPlayer(relay: RelayCtrl) {
  return relay.data.videoUrls
    ? renderVideoPlayer(relay)
    : relay.pinStreamer() && relay.allowPinnedImageOnUniboards()
    ? renderPinnedImage(relay)
    : undefined;
}

export function renderPinnedImage(relay: RelayCtrl) {
  if (!relay.pinStreamer() || !relay.data.pinned?.image) return undefined;
  return h('img.link', {
    attrs: { src: relay.data.pinned.image },
    hook: bind('click', () => {
      if (window.getComputedStyle(document.body).getPropertyValue('--allow-video') !== 'true') {
        const url = `${window.location.origin}/streamer/${relay.data.pinned!.userId}`;
        window.open(url, '_blank', 'noopener');
        return;
      }
      const url = new URL(location.href);
      url.searchParams.set('embed', relay.data.pinned!.userId);
      window.location.replace(url);
    }),
  });
}
