import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { render as renderGround } from './ground';
import { renderClock } from './clock/clockView';
import { renderInner as tableInner } from './view/table';
import renderCorresClock from './corresClock/corresClockView';
import { Position } from './interfaces';
import RoundController from './ctrl';

let handler: any;

export function view(ctrl: RoundController): VNode {
  return h('div.blind', [
    h('div.textual', {
      hook: {
        insert: vnode => init(vnode.elm as HTMLElement, ctrl)
      }
    }, [ renderGround(ctrl) ]),
    h('dt', 'Your clock'),
    h('dd.botc', anyClock(ctrl, 'bottom')),
    h('dt', 'Opponent clock'),
    h('dd.topc', anyClock(ctrl, 'top')),
    h('dt', 'Actions'),
    h('dd.actions', tableInner(ctrl)),
    h('dt', 'Board'),
    h('dd.board', h('pre'))
  ]);
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
