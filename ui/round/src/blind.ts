import { h } from 'snabbdom'
import { render as renderGround } from './ground';
import { renderClock } from './clock/clockView';
import renderCorresClock from './corresClock/corresClockView';
import { MaybeVNodes } from './interfaces';
import RoundController from './ctrl';

let handler: any;

export function view(ctrl: RoundController): MaybeVNodes {
  const d = ctrl.data,
    player = d.player;
  const clockView = (ctrl.clock && renderClock(ctrl, player, 'bottom')) || (
    (d.correspondence && d.game.turns > 1) &&
    renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, 'bottom', d.game.player)
  ) || 'none';
  return [
    h('div.lichess_board_blind', [
      h('div.textual', {
        hook: {
          insert: vnode => init(vnode.elm as HTMLElement, ctrl)
        }
      }, [ renderGround(ctrl) ]),
      h('dt', 'Clock'),
      h('dd', [clockView])
    ])
  ];
}

function init(el: HTMLElement, ctrl: RoundController) {
  if (window.lichess.NVUI) handler = window.lichess.NVUI(el, ctrl);
  else window.lichess.loadScript('compiled/nvui.min.js').then(() => init(el, ctrl));
}
export function reload() {
  if (handler) handler.reload();
}
