import stepwiseScroll from 'common/wheel';
import { VNode, h } from 'snabbdom';
import RoundController from '../ctrl';
import * as shogiground from '../ground';
import * as keyboard from '../keyboard';
import { render as keyboardMove } from '../keyboardMove';
import * as util from '../util';
import { renderTable } from './table';

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
          ctrl.data.game.variant.key === 'chushogi' ? null : shogiground.renderHand(ctrl, 'top'),
          ...renderTable(ctrl),
          ctrl.data.game.variant.key === 'chushogi' ? null : shogiground.renderHand(ctrl, 'bottom'),
          ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null,
        ]
      );
}
