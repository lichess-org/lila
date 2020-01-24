import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Thread, BaseMsg, User } from './interfaces'
import MsgCtrl from './ctrl';

function userIcon(user: User): VNode {
  return h('i.line' + (user.patron ? '.patron' : ''));
}

function userName(user: User): Array<string | VNode> {
  return user.title ? [
    h(
      'span.title',
      user.title == 'BOT' ? { attrs: {'data-bot': true } } : {},
      user.title
    ), ' ', user.name
  ] : [user.name];
}

function renderDate(msg: BaseMsg) {
  var date = new Date(msg.date);
  return h('time.timeago', {
    attrs: {
      title: date.toLocaleString(),
      datetime: msg.date
    }
  }, window.lichess.timeago.format(date));
}

function sideThread(thread: Thread) {
  return h('div.msg-app__threads__thread', [
    h('a.msg-app__threads__thread__icon.user-link.ulpt', {
      class: {
        online: thread.contact.online,
        offline: !thread.contact.online
      },
      attrs: {
        href: '/@/' + thread.contact.name
      }
    }, [userIcon(thread.contact)]),
    h('div.msg-app__threads__thread__contact', [
      h('div.msg-app__threads__thread__head', [
        h('a.msg-app__threads__thread__name', userName(thread.contact)),
        h('a.msg-app__threads__thread__date', renderDate(thread.lastMsg))
      ]),
      h('a.msg-app__threads__thread__msg', thread.lastMsg.text)
    ])
  ]);
}

export default function(ctrl: MsgCtrl) {
  return h('div.msg-app', [
    h('div.msg-app__side', [
      h('div.msg-app__side__search', [
        h('input')
      ]),
      h('div.msg-app__threads', [
        ctrl.data.threads.map(sideThread)
      ])
    ]),
    h('div.msg-app__main', [
      'main'
    ])
  ]);
}
