import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { RelayCtrl } from './relayCtrl';
import { iconTag, bind } from '../../util';

export default function(ctrl: RelayCtrl): VNode {
  const d = ctrl.data;
  return h('div.relay_wrap', [
    h('h2', d.pgnUrl),
    h('div.relay', [
      (d.syncSeconds ? stateOn : stateOff)(ctrl)
    ])
  ]);
}

function stateOn(ctrl: RelayCtrl) {
  return h('div.state.on.clickable', {
    hook: bind('click', _ => ctrl.setSync(false))
  }, [
    iconTag('E'),
    h('div', [
      'Currently connected to PGN source',
      h('div.timer', {
        hook: {
          insert: vnode => $(vnode.elm as HTMLElement).clock({ time: ctrl.data.syncSeconds! })
        }
      }, [
        h('span.shy', 'Will disconnect in '),
        h('span.time.text')
      ])
    ])
  ]);
}

function stateOff(ctrl: RelayCtrl) {
  return h('div.state.off.clickable', {
    hook: bind('click', _ => ctrl.setSync(true))
  }, [
    iconTag('G'),
    h('div', 'Connect to PGN source' )
  ]);
}
