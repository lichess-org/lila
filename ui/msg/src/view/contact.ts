import { h, type VNode } from 'snabbdom';
import type { Contact, LastMsg, User } from '../interfaces';
import type MsgCtrl from '../ctrl';
import * as licon from 'lib/licon';
import { hookMobileMousedown } from 'lib/device';
import { fullName, userLine } from 'lib/view/userLink';
import { timeago } from 'lib/i18n';
import type { MaybeVNodes } from 'lib/view/snabbdom';

export default function renderContact(ctrl: MsgCtrl, contact: Contact, active?: string): VNode {
  const user = contact.user,
    msg = contact.lastMsg,
    isNew = !msg.read && msg.user !== ctrl.data.me.id;
  return h(
    'div.msg-app__side__contact',
    {
      key: user.id,
      class: { active: active === user.id },
      hook: hookMobileMousedown(_ => ctrl.openConvo(user.id)),
    },
    [
      userIcon(user, 'msg-app__side__contact__icon'),
      h('div.msg-app__side__contact__user', [
        h('div.msg-app__side__contact__head', [
          h('div.msg-app__side__contact__name', contactName(user, ctrl)),
          h('div.msg-app__side__contact__date', renderDate(msg)),
        ]),
        h('div.msg-app__side__contact__body', [
          h(
            'div.msg-app__side__contact__msg',
            { class: { 'msg-app__side__contact__msg--new': isNew } },
            msg.text,
          ),
          isNew ? h('i.msg-app__side__contact__new', { attrs: { 'data-icon': licon.BellOutline } }) : null,
        ]),
      ]),
    ],
  );
}

const contactName = (user: User, ctrl: MsgCtrl): MaybeVNodes =>
  ctrl.data.names[user.id] ? [ctrl.data.names[user.id]] : fullName(user);

export const userIcon = (user: User, cls: string): VNode =>
  h('div.user-link.' + cls, { class: { online: user.online } }, userLine(user));

const renderDate = (msg: LastMsg): VNode =>
  h(
    'time.timeago',
    { key: msg.date.getTime(), attrs: { title: msg.date.toLocaleString(), datetime: msg.date.getTime() } },
    timeago(msg.date),
  );
