import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode';
import StormCtrl from '../ctrl';
// import { MaybeVNodes, StormPuzzle } from '../interfaces';

export default function(ctrl: StormCtrl) {
  return h('main.storm-app', [
    'UI here'
  ]);
}
