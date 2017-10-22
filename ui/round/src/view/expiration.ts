import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import RoundController from '../ctrl';

export default function(ctrl: RoundController): VNode | undefined {
  const d = ctrl.data.expiration;
  if (!d) return;
  const timeLeft = Math.max(0, d.movedAt - Date.now() + d.millisToMove);
  return h('div.expiration', [
    Math.round(timeLeft / 1000),
    ' seconds to move'
  ]);
}
