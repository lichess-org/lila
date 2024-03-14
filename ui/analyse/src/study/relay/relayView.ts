import { view as cevalView } from 'ceval';
import { onClickAway } from 'common';
import { looseH as h, bind, onInsert } from 'common/snabbdom';
import * as licon from 'common/licon';
import AnalyseCtrl from '../../ctrl';
import RelayCtrl from '../../study/relay/relayCtrl';
import { view as keyboardView } from '../../keyboard';
import type * as studyDeps from '../../study/studyDeps';
import { tourSide } from '../../study/relay/relayTourView';
import { renderVideoPlayer, resizeVideoPlayer } from './videoPlayerView';
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
    ctrl.keyboardHelp && keyboardView(ctrl), // all this still accurate?
    study && deps?.studyView.overboard(study),
    ...(tourUi ? renderTourView() : renderBoardView(ctx)),
  ]);
}

export function init(relay: RelayCtrl) {
  let cols = 0;

  window.addEventListener(
    'resize',
    () => {
      resizeVideoPlayer();
      const newCols = Number(window.getComputedStyle(document.body).getPropertyValue('--cols'));
      if (newCols === cols) return;
      cols = newCols;
      relay.redraw();
    },
    { passive: true },
  );
}

export function renderStreamerMenu(relay: RelayCtrl) {
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
      h(
        'div',
        relay.streams.map(([id, name]) =>
          h(
            'button.button-empty.streamer',
            { attrs: { 'data-icon': licon.Mic }, hook: bind('click', () => relay.showStream(id)) },
            name,
          ),
        ),
      ),
    ),
  );
}

function renderBoardView(ctx: ViewContext) {
  const { ctrl, deps, study, gaugeOn, relay } = ctx;
  // no worries about FOUC when overriding grid constraints here, it's all snab
  const cols = Number(window.getComputedStyle(document.body).getPropertyValue('--cols'));
  if (cols >= 2)
    return [
      renderBoard(ctx),
      gaugeOn && cevalView.renderGauge(ctrl),
      cols === 3 && tourSide(ctrl, study!, relay!),
      h('div.flex-side', [
        cols === 3 ? renderVideoPlayer(relay!) : tourSide(ctrl, study!, relay!),
        renderTools(ctx),
        renderControls(ctrl),
        deps?.relayManager(relay!, study!),
      ]),
      renderUnderboard(ctx),
    ];
  else
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
