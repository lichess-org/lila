import { h } from 'snabbdom';
import type RoundController from '../ctrl';
import { isPlayerTurn, playable } from 'game';
import type { MaybeVNode } from 'common/snabbdom';

let rang = false;

export default function (ctrl: RoundController): MaybeVNode {
  const d = playable(ctrl.data) && ctrl.data.expiration;
  if (!d) return;
  const timeLeft = Math.max(0, d.movedAt - Date.now() + d.millisToMove),
    secondsLeft = Math.floor(timeLeft / 1000),
    myTurn = isPlayerTurn(ctrl.data),
    emerg = myTurn && timeLeft < 8000;
  if (!rang && emerg) {
    site.sound.play('lowTime');
    rang = true;
  }
  const side = myTurn !== ctrl.flip ? 'bottom' : 'top';
  return h(
    'div.expiration.expiration-' + side,
    { class: { emerg, 'bar-glider': myTurn } },
    i18n.site.nbSecondsToPlayTheFirstMove.asArray(secondsLeft, h('strong', '' + secondsLeft)),
  );
}
