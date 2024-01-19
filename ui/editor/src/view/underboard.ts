import { VNode, h } from 'snabbdom';
import EditorCtrl from '../ctrl';
import { EditorState } from '../interfaces';
import { inputs } from './inputs';
import { tools } from './tools';

export function underboard(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.underboard', [inputs(ctrl, state.sfen), tools(ctrl, state)]);
}
