import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Thread, BaseMsg } from '../interfaces'
import MsgCtrl from '../ctrl';
import { userName, userIcon, bindMobileMousedown } from './util';

export default function renderThread(ctrl: MsgCtrl, thread: Thread, active?: string): VNode {
  const msg = thread.lastMsg;
  return h('div.msg-app__side__thread', {
    key: thread.id,
    class: {
      active: active == thread.id,
      new: !!msg && !msg.read && msg.user != ctrl.data.me.id
    },
    hook: bindMobileMousedown(_ => ctrl.openConvo(thread.contact.id)),
  }, [
    userIcon(thread.contact, 'msg-app__side__thread__icon'),
    h('div.msg-app__side__thread__contact', [
      h('div.msg-app__side__thread__head', [
        h('div.msg-app__side__thread__name', userName(thread.contact)),
        msg ? h('div.msg-app__side__thread__date', renderDate(msg)) : null
      ]),
      msg ? h('div.msg-app__side__thread__msg', msg.text) : null
    ])
  ]);
}

function renderDate(msg: BaseMsg): VNode {
  return h('time.timeago', {
    key: msg.date.getTime(),
    attrs: {
      title: msg.date.toLocaleString(),
      datetime: msg.date.getTime()
    }
  }, window.lichess.timeago.format(msg.date));
}
