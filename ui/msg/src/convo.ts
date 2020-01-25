import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo, ConvoMsg } from './interfaces'
import { userName, userIcon } from './util';
import MsgCtrl from './ctrl';

export default function renderConvo(ctrl: MsgCtrl, convo: Convo): VNode {
  return h('div.msg-app__convo', {
    key: convo.thread.contact.id
  }, [
    h('div.msg-app__convo__head', [
      h('div.msg-app__convo__head__contact', [
        userIcon(convo.thread.contact, 'msg-app__convo__head__icon'),
        h('div.msg-app__convo__head__name', userName(convo.thread.contact))
      ])
    ]),
    h('div.msg-app__convo__msgs', {
      hook: {
        insert(vnode) {
          (vnode.elm as HTMLElement).scrollTop = 9999999;
        }
      }
    }, [
      h('div.msg-app__convo__msgs__init'),
      h('div.msg-app__convo__msgs__content', renderMsgs(ctrl, convo))
    ]),
    h('div.msg-app__convo__reply', [
      h('textarea.msg-app__convo__reply__text', {
        attrs: {
          rows: 1,
          autofocus: 1
        }
      })
    ])
  ]);
}

function renderMsgs(ctrl: MsgCtrl, convo: Convo): VNode[] {
  return convo.msgs.map(msg => renderMsg(ctrl, msg));
}

function renderMsg(ctrl: MsgCtrl, msg: ConvoMsg) {
  const cls = msg.user == ctrl.data.me.id ? 'mine' : 'them';
  const date = new Date(msg.date);
  return h('div.msg.' + cls, [
    h('p', msg.text),
    h('em', [
      date.getHours(),
      ':',
      date.getMinutes()
    ])
  ]);
}
