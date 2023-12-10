import { h } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';

export type Close = () => void;

export const header = (name: string, close: Close) =>
  h(
    'button.head.text',
    {
      attrs: { 'data-icon': licon.LessThan, type: 'button' },
      hook: bind('click', close),
    },
    name,
  );
