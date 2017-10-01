import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import { RelayCtrl, LogEvent } from './relayCtrl';
import { iconTag, bind } from '../../util';

export default function(ctrl: RelayCtrl): VNode {
  const d = ctrl.data;
  return h('div.relay_wrap', [
    h('h2', 'Relay manager'),
    h('div.relay', [
      (d.sync.seconds ? stateOn : stateOff)(ctrl),
      renderLog(d.sync.log)
    ])
  ]);
}

function renderLog(log: LogEvent[]) {
  return h('div.log', log.slice(0).reverse().map(e => {
    const err = e.error;
    return h('div' + (err ? '.err' : ''), {
      key: e.at
    }, [
      iconTag(err ? 'j' : 'E'),
      h('div', [
        err || 'Success',
        h('time', window.lichess.timeago.absolute(e.at))
      ])
    ]);
  }));
}

function stateOn(ctrl: RelayCtrl) {
  return h('div.state.on.clickable', {
    hook: bind('click', _ => ctrl.setSync(false))
  }, [
    iconTag('B'),
    h('div', [
      'Connected to PGN source',
      h('div.timer', {
        hook: {
          insert: vnode => $(vnode.elm as HTMLElement).clock({ time: ctrl.data.sync.seconds! })
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
    h('div', [
      'Click to connect',
      h('br'),
      'to PGN source'
    ])
  ]);
}
