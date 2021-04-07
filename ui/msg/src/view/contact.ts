import { h, VNode } from 'snabbdom';
import { Contact, LastMsg } from '../interfaces';
import MsgCtrl from '../ctrl';
import { userName, userIcon, bindMobileMousedown } from './util';

export default function renderContact(ctrl: MsgCtrl, contact: Contact, active?: string): VNode {
  const user = contact.user,
    msg = contact.lastMsg,
    isNew = !msg.read && msg.user != ctrl.data.me.id;
  return h(
    'div.msg-app__side__contact',
    {
      key: user.id,
      class: { active: active == user.id },
      hook: bindMobileMousedown(_ => ctrl.openConvo(user.id)),
    },
    [
      userIcon(user, 'msg-app__side__contact__icon'),
      h('div.msg-app__side__contact__user', [
        h('div.msg-app__side__contact__head', [
          h('div.msg-app__side__contact__name', userName(user)),
          h('div.msg-app__side__contact__date', renderDate(msg)),
        ]),
        h('div.msg-app__side__contact__body', [
          h(
            'div.msg-app__side__contact__msg',
            {
              class: { 'msg-app__side__contact__msg--new': isNew },
            },
            msg.text
          ),
          isNew
            ? h('i.msg-app__side__contact__new', {
                attrs: { 'data-icon': '' },
              })
            : null,
        ]),
      ]),
    ]
  );
}

function renderDate(msg: LastMsg): VNode {
  return h(
    'time.timeago',
    {
      key: msg.date.getTime(),
      attrs: {
        title: msg.date.toLocaleString(),
        datetime: msg.date.getTime(),
      },
    },
    lichess.timeago(msg.date)
  );
}
