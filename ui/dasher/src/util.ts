import { h } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { memoize } from 'common';

export type Close = () => void;

export const header = (name: string, close: Close) =>
  h(
    'button.head.text',
    { attrs: { 'data-icon': licon.LessThan, type: 'button' }, hook: bind('click', close) },
    name,
  );

export const elementScrollBarWidth = memoize<number>(() => {
  const ruler = document.createElement('div');
  ruler.style.position = 'absolute';
  ruler.style.overflow = 'scroll';
  ruler.style.visibility = 'hidden';
  document.body.appendChild(ruler);
  const barWidth = ruler.offsetWidth - ruler.clientWidth;
  document.body.removeChild(ruler);
  return barWidth;
});
