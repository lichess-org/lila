import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Convo } from '../interfaces'
import { userName } from './util';
import renderMsgs from './msgs';
import renderActions from './actions';
import renderTextarea from './textarea';
import MsgCtrl from '../ctrl';

export default function renderConvo(ctrl: MsgCtrl, convo: Convo): VNode {
  const user = convo.thread.contact;
  return h('div.msg-app__convo', {
    key: `${user.id}:${convo.msgs[0] && convo.msgs[0].date.getDate()}`,
  }, [
    h('div.msg-app__convo__head', [
      h('a.user-link.ulpt', {
        attrs: { href: `/@/${user.name}` },
        class: {
          online: user.online,
          offline: !user.online
        }
      }, [
        h('i.line' + (user.patron ? '.patron' : '')),
        ...userName(user)
      ]),
      h('div.msg-app__convo__head__actions', renderActions(ctrl, convo))
    ]),
    renderMsgs(ctrl, convo.msgs),
    h('div.msg-app__convo__reply', [
      convo.relations.out === false || convo.relations.in === false ?
        h('div.msg-app__convo__reply__block.text', {
          attrs: { 'data-icon': 'k' }
        }, 'This conversation is blocked.') :
        renderTextarea(ctrl, user)
    ])
  ]);
}
