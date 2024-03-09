import stepwiseScroll from 'common/wheel';
import { VNode, h } from 'snabbdom';
import RoundController from '../ctrl';
import * as shogiground from '../ground';
import * as keyboard from '../keyboard';
import { render as keyboardMove } from 'keyboardMove';
import * as util from '../util';
import { renderTable } from './table';

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data;

  return ctrl.nvui
    ? ctrl.nvui.render(ctrl)
    : h(
        'div.' + ctrl.opts.klasses.join('.'),
        {
          class: { 'move-confirm': !!ctrl.usiToSubmit },
        },
        [
          h(
            'div.round__app__board.main-board' +
              `.v-${d.game.variant.key}` +
              (ctrl.data.pref.blindfold ? '.blindfold' : ''),
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
          ...renderTable(ctrl),
          ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null,
        ]
      );
}
