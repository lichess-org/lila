import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import RelayCtrl from './relayCtrl';
import { RelayData } from './interfaces';
import { iconTag, bind } from '../../util';

export default function(ctrl: RelayCtrl): VNode | undefined {
  const d = ctrl.data;
  if (ctrl.members.canContribute()) return h('div.relay_wrap', [
    h('h2', [
      'Relay manager',
      ctrl.members.isOwner() ? h('a', {
        attrs: {
          href: `/relay/${d.slug}/${d.id}/edit`,
          'data-icon': '%'
        }
      }) : null
    ]),
    h('div.relay', [
      (d.sync.seconds ? stateOn : stateOff)(ctrl),
      renderLog(d)
    ])
  ]);
}

function renderLog(d: RelayData) {
  return h('div.log', d.sync.log.slice(0).reverse().map(e => {
    const err = e.error && h('a', {
      attrs: {
        href: d.sync.url,
        target: '_blank'
      }
    }, e.error);
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
