import { opposite } from 'shogiground/util';
import { VNode, h } from 'snabbdom';
import EditorCtrl from '../ctrl';
import * as ground from '../shogiground';
import { sparePieces } from './spares';
import { actions } from './actions';
import { underboard } from './underboard';
import { links } from './links';
import { tools } from './tools';

export default function (ctrl: EditorCtrl): VNode {
  const state = ctrl.getState(),
    color = ctrl.bottomColor();
  return h(`div.board-editor.main-v-${ctrl.rules}`, [
    sparePieces(ctrl, opposite(color), 'top'),
    h(`div.main-board.v-${ctrl.rules}`, [ground.renderBoard(ctrl)]),
    sparePieces(ctrl, color, 'bottom'),
    actions(ctrl, state),
    !ctrl.data.embed ? links(ctrl, state) : null,
    !ctrl.data.embed ? underboard(ctrl, state) : tools(ctrl, state),
  ]);
}
