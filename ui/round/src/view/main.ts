import * as keyboard from '../keyboard';
import * as util from '../util';
import crazyView from '../crazy/crazyView';
import RoundController from '../ctrl';
import { stepwiseScroll } from 'common/scroll';
import { VNode } from 'snabbdom';
import { looseH as h } from 'common/snabbdom';
import { render as renderKeyboardMove } from 'keyboardMove';
import { render as renderGround } from '../ground';
import { renderTable } from './table';
import { renderMaterialDiffs } from 'game/view/material';
import { renderVoiceBar } from 'voice';
import { playable } from 'game';

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
    topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
    bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color,
    materialDiffs = renderMaterialDiffs(
      ctrl.data.pref.showCaptured,
      ctrl.flip ? ctrl.data.opponent.color : ctrl.data.player.color,
      ctrl.stepAt(ctrl.ply).fen,
      !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
      ctrl.data.steps,
      ctrl.ply,
    );
  const hideBoard = ctrl.data.player.blindfold && playable(ctrl.data);
  return ctrl.nvui
    ? ctrl.nvui.render(ctrl)
    : h('div.round__app.variant-' + d.game.variant.key, [
        h(
          'div.round__app__board.main-board' + (hideBoard ? '.blindfold' : ''),
          {
            hook:
              'ontouchstart' in window || !site.storage.boolean('scrollMoves').getOrDefault(true)
                ? undefined
                : util.bind(
                    'wheel',
                    stepwiseScroll((e: WheelEvent, scroll: boolean) => {
                      if (!ctrl.isPlaying()) {
                        e.preventDefault();
                        if (e.deltaY > 0 && scroll) keyboard.next(ctrl);
                        else if (e.deltaY < 0 && scroll) keyboard.prev(ctrl);
                        ctrl.redraw();
                      }
                    }),
                    undefined,
                    false,
                  ),
          },
          [renderGround(ctrl), ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess')],
        ),
        ctrl.voiceMove && renderVoiceBar(ctrl.voiceMove.ui, ctrl.redraw),
        ctrl.keyboardHelp && keyboard.view(ctrl),
        crazyView(ctrl, topColor, 'top') || materialDiffs[0],
        ...renderTable(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || materialDiffs[1],
        ctrl.keyboardMove && renderKeyboardMove(ctrl.keyboardMove),
      ]);
}

export function endGameView() {
  if ($('body').hasClass('zen-auto') && $('body').hasClass('zen')) {
    $('body').toggleClass('zen');
    window.dispatchEvent(new Event('resize'));
  }
}
