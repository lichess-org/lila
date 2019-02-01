import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import RoundController from '../ctrl';
import { isPlayerTurn } from 'game';

let rang = false;

export default function(ctrl: RoundController): [VNode, boolean] | undefined {
  const d = ctrl.data.expiration;
  if (!d) return;
  const timeLeft = Math.max(0, d.movedAt - Date.now() + d.millisToMove),
    secondsLeft = Math.floor(timeLeft / 1000),
    myTurn = isPlayerTurn(ctrl.data),
    emerg = myTurn && timeLeft < 8000;
  if (!rang && emerg) {
    window.lichess.sound.lowtime();
    rang = true;
  }
  return [
    h('div.expiration.suggestion', {
      class: { emerg }
    }, ctrl.trans.vdomPlural('nbSecondsToPlayTheFirstMove', secondsLeft, h('strong', '' + secondsLeft))),
    myTurn
  ];
}
