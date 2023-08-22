import { h, VNode } from 'snabbdom';
import { User } from '../interfaces';

export function userIcon(user: User, cls: string): VNode {
  return h(
    'div.user-link.' + cls,
    {
      class: {
        online: user.online,
        offline: !user.online,
      },
    },
    [h('i.line' + (user.patron ? '.patron' : ''))],
  );
}

export const userName = (user: User): Array<string | VNode> =>
  user.title
    ? [
        h('span.utitle', user.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, user.title),
        ' ',
        user.name,
      ]
    : [user.name];
