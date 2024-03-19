import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, onInsert } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../../study/studyDeps';
import { tourSide } from '../../study/relay/relayTourView';
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
) {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay };

  const renderTourView = (uw: boolean) => [
    ctx.tourUi,
    ...tourSide(ctrl, study, relay, uw),
    deps.relayManager(relay, study),
  ];
  const scale =
    (parseFloat(window.getComputedStyle(document.body).getPropertyValue('--zoom')) / 100) * 0.75 + 0.25;

  const leftOver = window.innerWidth - 350 - 60 - scale * window.innerHeight;
  const ultraWide = leftOver > 700;
  const classes = ultraWide ? ['ultra-wide'] : [];
  if (ultraWide && relay.data.videoUrls) classes.push('with-video');
  return renderMain(ctx, classes, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ...(ctx.tourUi ? renderTourView(ultraWide) : renderBoardView(ctx, ultraWide)),
  ]);
}

export function onWindowResize(redraw: () => void) {
  let showingVideo = false;
  let wasUltraWide = false;
  window.addEventListener(
    'resize',
    () => {
      const scale =
        (parseFloat(window.getComputedStyle(document.body).getPropertyValue('--zoom')) / 100) * 0.75 + 0.25;

      const leftOver = window.innerWidth - 350 - 60 - scale * window.innerHeight;
      const ultraWide = leftOver > 700;

      const allow = window.getComputedStyle(document.body).getPropertyValue('--allow-video') === 'true';
      const placeholder = document.getElementById('video-player-placeholder') ?? undefined;
      player?.cover(allow ? placeholder : undefined);
      if (showingVideo === (allow && !!placeholder) && ultraWide === wasUltraWide) return;
      wasUltraWide = ultraWide;
      showingVideo = allow && !!placeholder;
      console.log('redraw');
      redraw();
    },
    { passive: true },
  );
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

function renderBoardView(ctx: RelayViewContext, uw: boolean) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;

  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    uw && renderVideoPlayer(relay),
    renderTools(ctx, !uw ? renderVideoPlayer(relay) : undefined),
    renderControls(ctrl),
    renderUnderboard(ctx),
    ...tourSide(ctrl, study, relay, uw),
    deps.relayManager(relay, study),
  ];
}
