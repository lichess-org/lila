import { h, VNode } from 'snabbdom';
import { renderTable } from './table';
import * as shogiground from '../ground';
import * as util from '../util';
import * as keyboard from '../keyboard';
import { render as keyboardMove } from '../keyboardMove';
import RoundController from '../ctrl';
import stepwiseScroll from 'common/wheel';

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data;

  return ctrl.nvui
    ? ctrl.nvui.render(ctrl)
    : h(
        'div.round__app.variant-' + d.game.variant.key,
        {
          class: { 'move-confirm': !!ctrl.usiToSubmit },
        },
        [
          h(
            'div.round__app__board.main-board' + (ctrl.data.pref.blindfold ? '.blindfold' : ''),
            {
              hook:
                window.lishogi.hasTouchEvents || window.lishogi.storage.get('scrollMoves') == '0'
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
                      false
                    ),
            },
            shogiground.renderBoard(ctrl)
          ),
          shogiground.renderHand(ctrl, 'top'),
          ...renderTable(ctrl),
          shogiground.renderHand(ctrl, 'bottom'),
          ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null,
        ]
      );
}
