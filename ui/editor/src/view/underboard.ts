import { type VNode, h } from 'snabbdom';
import type EditorCtrl from '../ctrl';
import type { EditorState } from '../interfaces';
import { inputs } from './inputs';
import { tools } from './tools';

export function underboard(ctrl: EditorCtrl, state: EditorState): VNode {
  return h('div.underboard', [inputs(ctrl, state.sfen), tools(ctrl, state)]);
}
