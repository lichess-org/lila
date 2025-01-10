import { opposite } from 'shogiground/util';
import { type VNode, h } from 'snabbdom';
import type EditorCtrl from '../ctrl';
import * as ground from '../shogiground';
import { actions } from './actions';
import { links } from './links';
import { sparePieces } from './spares';
import { tools } from './tools';
import { underboard } from './underboard';

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
