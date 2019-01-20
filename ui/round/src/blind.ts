import { h } from 'snabbdom'
import { render as renderGround } from './ground';
import { renderClock } from './clock/clockView';
import renderCorresClock from './corresClock/corresClockView';
import { MaybeVNodes, Position } from './interfaces';
import RoundController from './ctrl';

let handler: any;

export function view(ctrl: RoundController): MaybeVNodes {
  return [
    h('div.textual', {
      hook: {
        insert: vnode => init(vnode.elm as HTMLElement, ctrl)
      }
    }, [ renderGround(ctrl) ]),
    h('dt', 'Your clock'),
    h('dd', anyClock(ctrl, 'bottom')),
    h('dt', 'Opponent clock'),
    h('dd', anyClock(ctrl, 'top'))
  ];
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data, player = ctrl.playerAt(position);
  return (ctrl.clock && renderClock(ctrl, player, position)) || (
    d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)
  ) || 'none';
}

function init(el: HTMLElement, ctrl: RoundController) {
  if (window.lichess.NVUI) handler = window.lichess.NVUI(el, ctrl);
  else window.lichess.loadScript('compiled/nvui.min.js').then(() => init(el, ctrl));
}
export function reload() {
  if (handler) handler.reload();
}
