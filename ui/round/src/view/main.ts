import * as keyboard from '../keyboard';
import * as util from '../util';
import crazyView from '../crazy/crazyView';
import RoundController from '../ctrl';
import { h, VNode } from 'snabbdom';
import { plyStep } from '../round';
import { read as readFen } from 'chessground/fen';
import { render as keyboardMove } from '../keyboardMove';
import { render as renderGround } from '../ground';
import { renderTable } from './table';
import { renderMaterialDiffs } from 'game/view/material';

function wheel(ctrl: RoundController, e: WheelEvent): void {
  if (!ctrl.isPlaying()) {
    e.preventDefault();
    if (e.deltaY > 0) keyboard.next(ctrl);
    else if (e.deltaY < 0) keyboard.prev(ctrl);
    ctrl.redraw();
  }
}

export function main(ctrl: RoundController): VNode {
  const d = ctrl.data,
    cgState = ctrl.chessground && ctrl.chessground.state,
    topColor = d[ctrl.flip ? 'player' : 'opponent'].color,
    bottomColor = d[ctrl.flip ? 'opponent' : 'player'].color,
    pieces = cgState ? cgState.pieces : readFen(plyStep(ctrl.data, ctrl.ply).fen),
    materialDiffs = renderMaterialDiffs(
      ctrl.data.pref.showCaptured,
      ctrl.flip ? ctrl.data.opponent.color : ctrl.data.player.color,
      pieces,
      !!(ctrl.data.player.checks || ctrl.data.opponent.checks), // showChecks
      ctrl.data.steps,
      ctrl.ply
    );

  return ctrl.nvui
    ? ctrl.nvui.render(ctrl)
    : h('div.round__app.variant-' + d.game.variant.key, [
        h(
          'div.round__app__board.main-board' + (ctrl.data.pref.blindfold ? '.blindfold' : ''),
          {
            hook:
              'ontouchstart' in window || lichess.storage.get('scrollMoves') == '0'
                ? undefined
                : util.bind('wheel', (e: WheelEvent) => wheel(ctrl, e), undefined, false),
          },
          [renderGround(ctrl), ctrl.promotion.view(ctrl.data.game.variant.key === 'antichess')]
        ),
        crazyView(ctrl, topColor, 'top') || materialDiffs[0],
        ...renderTable(ctrl),
        crazyView(ctrl, bottomColor, 'bottom') || materialDiffs[1],
        ctrl.keyboardMove ? keyboardMove(ctrl.keyboardMove) : null,
      ]);
}
