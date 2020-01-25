import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Thread, BaseMsg } from './interfaces'
import { userName, userIcon, bindMobileMousedown } from './util';
import MsgCtrl from './ctrl';
import renderConvo from './convo';

function renderDate(msg: BaseMsg): VNode {
  var date = new Date(msg.date);
  return h('time.timeago', {
    attrs: {
      title: date.toLocaleString(),
      datetime: msg.date
    }
  }, window.lichess.timeago.format(date));
}

function sideThread(ctrl: MsgCtrl, thread: Thread, active?: string): VNode {
  return h('div.msg-app__threads__thread', {
    class: { active: active == thread.id },
    hook: bindMobileMousedown(_ => ctrl.openThread(thread.contact.id)),
  }, [
    userIcon(thread.contact, 'msg-app__threads__thread__icon'),
    h('div.msg-app__threads__thread__contact', [
      h('div.msg-app__threads__thread__head', [
        h('div.msg-app__threads__thread__name', userName(thread.contact)),
        thread.lastMsg ? h('div.msg-app__threads__thread__date', renderDate(thread.lastMsg)) : null
      ]),
      thread.lastMsg ? h('div.msg-app__threads__thread__msg', thread.lastMsg.text) : null
    ])
  ]);
}

export default function(ctrl: MsgCtrl): VNode {
  const activeId = ctrl.data.convo && ctrl.data.convo.thread.id;
  return h('main.msg-app.box', [
    h('div.msg-app__side', [
      h('div.msg-app__side__search', [
        h('input', {
          attrs: {
            placeholder: 'Search or start new chat'
          }
        })
      ]),
      h('div.msg-app__threads', ctrl.data.threads.map(t => sideThread(ctrl, t, activeId)))
    ]),
    h('div.msg-app__convo', ctrl.data.convo ? renderConvo(ctrl, ctrl.data.convo) : [])
  ]);
}
