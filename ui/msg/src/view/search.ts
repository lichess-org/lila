import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import throttle from 'common/throttle';
import MsgCtrl from '../ctrl';
import { SearchRes, User } from '../interfaces';
import renderContacts from './contact';
import { userName, userIcon, bindMobileMousedown } from './util';

export function renderInput(ctrl: MsgCtrl): VNode {
  return h('div.msg-app__side__search', [
    h('input', {
      attrs: {
        placeholder: 'Search or start new discussion'
      },
      hook: {
        insert(vnode) {
          const input = (vnode.elm as HTMLInputElement);
          input.addEventListener('input', throttle(500, () => ctrl.search(input.value.trim())));
          // input.addEventListener('blur', () => {
          //   input.value = '';
          //   ctrl.search('')
          // });
        }
      }
    })
  ]);
}

export function renderResults(ctrl: MsgCtrl, res: SearchRes): VNode {
  return h('div.msg-app__search.msg-app__side__content', [
    res.contacts[0] && h('section', [
      h('h2', 'Discussions'),
      h('div.msg-app__search__contacts', res.contacts.map(t => renderContacts(ctrl, t)))
    ]),
    res.friends[0] && h('section', [
      h('h2', 'Friends'),
      h('div.msg-app__search__users', res.friends.map(u => renderUser(ctrl, u)))
    ]),
    res.users[0] && h('section', [
      h('h2', 'Players'),
      h('div.msg-app__search__users', res.users.map(u => renderUser(ctrl, u)))
    ])
  ]);
}

function renderUser(ctrl: MsgCtrl, user: User): VNode {
  return h('div.msg-app__side__contact', {
    key: user.id,
    hook: bindMobileMousedown(_ => ctrl.openConvo(user.id)),
  }, [
    userIcon(user, 'msg-app__side__contact__icon'),
    h('div.msg-app__side__contact__user', [
      h('div.msg-app__side__contact__head', [
        h('div.msg-app__side__contact__name', userName(user))
      ])
    ])
  ]);
}
