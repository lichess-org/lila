import { h, VNode } from 'snabbdom';
import { User } from '../interfaces';

export const userName = (user: User): Array<string | VNode> =>
  user.title
    ? [
        h('span.utitle', user.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, user.title),
        ' ',
        user.name,
      ]
    : [user.name];
