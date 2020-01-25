import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Thread, BaseMsg } from './interfaces'
import MsgCtrl from './ctrl';
import { userName, userIcon, bindMobileMousedown } from './util';

export default function renderThread(ctrl: MsgCtrl, thread: Thread, active?: string): VNode {
  return h('div.msg-app__side__thread', {
    key: thread.id,
    class: {
      active: active == thread.id,
      new: !!thread.lastMsg && !thread.lastMsg.read && thread.lastMsg.user != ctrl.data.me.id
    },
    hook: bindMobileMousedown(_ => ctrl.openConvo(thread.contact.id)),
  }, [
    userIcon(thread.contact, 'msg-app__side__thread__icon'),
    h('div.msg-app__side__thread__contact', [
      h('div.msg-app__side__thread__head', [
        h('div.msg-app__side__thread__name', userName(thread.contact)),
        thread.lastMsg ? h('div.msg-app__side__thread__date', renderDate(thread.lastMsg)) : null
      ]),
      thread.lastMsg ? h('div.msg-app__side__thread__msg', thread.lastMsg.text) : null
    ])
  ]);
}

function renderDate(msg: BaseMsg): VNode {
  return h('time.timeago', {
    attrs: {
      title: msg.date.toLocaleString(),
      datetime: msg.date.getTime()
    }
  }, window.lichess.timeago.format(msg.date));
}
