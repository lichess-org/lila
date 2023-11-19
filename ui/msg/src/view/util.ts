import { h, VNode } from 'snabbdom';
import { User } from '../interfaces';
import { userLine, userTitle, userFlair } from 'common/userLink';
import { MaybeVNodes } from 'common/snabbdom';

export const userIcon = (user: User, cls: string): VNode =>
  h(
    'div.user-link.' + cls,
    {
      class: {
        online: user.online,
      },
    },
    userLine(user),
  );

export const userName = (user: User): MaybeVNodes => [userTitle(user), user.name, userFlair(user)];
