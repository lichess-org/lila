import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, onInsert, bind } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../../study/studyDeps';
import { tourSide } from '../../study/relay/relayTourView';
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
) {
  const ctx: RelayViewContext = { ...viewContext(ctrl, deps), study, deps, relay };

  const renderTourView = () => [ctx.tourUi, tourSide(ctrl, study, relay), deps.relayManager(relay, study)];

  return renderMain(ctx, [
    ctrl.keyboardHelp && keyboardView(ctrl),
    deps.studyView.overboard(study),
    ...(ctx.tourUi ? renderTourView() : renderBoardView(ctx)),
  ]);
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

export function renderPinnedImage(relay: RelayCtrl) {
  if (!relay.pinStreamer() || !relay.data.pinned?.image) return undefined;
  return h('img.link', {
    attrs: { src: relay.data.pinned.image },
    hook: bind('click', () => {
      if (window.getComputedStyle(document.body).getPropertyValue('--allow-video') !== 'true') {
        const url = `${window.location.origin}/streamer/${relay.data.pinned!.userId}`;
        window.open(url, '_blank', 'noopener'); //if (!window.open(url, '_blank', 'noopener')) window.location.href = url; //safari
        return;
      }
      const url = new URL(location.href);
      url.searchParams.set('embed', relay.data.pinned!.userId);
      window.location.replace(url);
    }),
  });
}

function renderBoardView(ctx: RelayViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  return [
    renderBoard(ctx),
    gaugeOn && cevalView.renderGauge(ctrl),
    renderTools(
      ctx,
      relay.data.videoUrls
        ? renderVideoPlayer(relay)
        : relay.isShowingPinnedImage()
        ? renderPinnedImage(relay)
        : undefined,
    ),
    renderControls(ctrl),
    renderUnderboard(ctx),
    tourSide(ctrl, study, relay),
    deps.relayManager(relay, study),
  ];
}
