import type { MaybeVNode } from 'common/snabbdom';
import { isPlayerTurn, playable } from 'game';
import { i18nVdomPlural } from 'i18n';
import { h } from 'snabbdom';
import type RoundController from '../ctrl';

let rang = false;

export default function (ctrl: RoundController): MaybeVNode {
  const d = playable(ctrl.data) && ctrl.data.expiration;
  if (!d) return;
  const timeLeft = Math.max(0, d.movedAt - Date.now() + d.millisToMove);
  const secondsLeft = Math.floor(timeLeft / 1000);
  const myTurn = isPlayerTurn(ctrl.data);
  const emerg = myTurn && timeLeft < 8000;
  if (!rang && emerg) {
    window.lishogi.sound.play('lowtime');
    rang = true;
  }
  const side = myTurn != ctrl.flip ? 'bottom' : 'top';
  return h(
    `div.expiration.expiration-${side}`,
    {
      class: {
        emerg,
        'bar-glider': myTurn,
      },
    },
    i18nVdomPlural('nbSecondsToPlayTheFirstMove', secondsLeft, h('strong', `${secondsLeft}`)),
  );
}
