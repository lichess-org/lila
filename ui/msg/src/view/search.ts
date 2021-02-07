import { h } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import throttle from 'common/throttle';
import MsgCtrl from '../ctrl';
import { SearchResult, User } from '../interfaces';
import renderContacts from './contact';
import { userName, userIcon, bindMobileMousedown } from './util';

export function renderInput(ctrl: MsgCtrl): VNode {
  return h('div.msg-app__side__search', [
    ctrl.data.me.kid
      ? null
      : h('input', {
          attrs: {
            value: '',
            placeholder: ctrl.trans.noarg('searchOrStartNewDiscussion'),
          },
          hook: {
            insert(vnode) {
              const input = vnode.elm as HTMLInputElement;
              input.addEventListener(
                'input',
                throttle(500, () => ctrl.searchInput(input.value.trim()))
              );
              input.addEventListener('blur', () =>
                setTimeout(() => {
                  input.value = '';
                  ctrl.searchInput('');
                }, 500)
              );
            },
          },
        }),
  ]);
}

export function renderResults(ctrl: MsgCtrl, res: SearchResult): VNode {
  return h('div.msg-app__search.msg-app__side__content', [
    res.contacts[0] &&
      h('section', [
        h('h2', ctrl.trans.noarg('discussions')),
        h(
          'div.msg-app__search__contacts',
          res.contacts.map(t => renderContacts(ctrl, t))
        ),
      ]),
    res.friends[0] &&
      h('section', [
        h('h2', ctrl.trans.noarg('friends')),
        h(
          'div.msg-app__search__users',
          res.friends.map(u => renderUser(ctrl, u))
        ),
      ]),
    res.users[0] &&
      h('section', [
        h('h2', ctrl.trans.noarg('players')),
        h(
          'div.msg-app__search__users',
          res.users.map(u => renderUser(ctrl, u))
        ),
      ]),
  ]);
}

function renderUser(ctrl: MsgCtrl, user: User): VNode {
  return h(
    'div.msg-app__side__contact',
    {
      key: user.id,
      hook: bindMobileMousedown(_ => ctrl.openConvo(user.id)),
    },
    [
      userIcon(user, 'msg-app__side__contact__icon'),
      h('div.msg-app__side__contact__user', [
        h('div.msg-app__side__contact__head', [h('div.msg-app__side__contact__name', userName(user))]),
      ]),
    ]
  );
}
