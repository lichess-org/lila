import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { RelayCtrl, RelayData } from './relayCtrl';
import { iconTag, bind } from '../../util';

export default function(ctrl: RelayCtrl): VNode {
  const d = ctrl.data;
  return h('div.relay_manage', [
    (d.syncSeconds ? stateOn : stateOff)(ctrl)
  ]);
}

function source(d: RelayData) {
  return h('a', {
    attrs: { href: d.pgnUrl }
  }, 'PGN source');
}

function stateOn(ctrl: RelayCtrl) {
  return h('div.state.on', [
    iconTag('E'),
    h('h2', [
      'Currently connected to ',
      source(ctrl.data)
    ]),
    h('button.button', {
      hook: bind('click', _ => ctrl.setSync(false))
    }, 'Pause')
  ]);
}

function stateOff(ctrl: RelayCtrl) {
  return h('div.state.off', [
    iconTag('G'),
    h('button.button', {
      hook: bind('click', _ => ctrl.setSync(true))
    }, [
      'Connect to ',
      source(ctrl.data)
    ])
  ]);
}
