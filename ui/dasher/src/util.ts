import * as licon from 'common/licon';
import { bind, looseH as h, type VNode } from 'common/snabbdom';
import { memoize } from 'common';

export const header = (name: string, close: () => void): VNode =>
  h(
    'button.head.text',
    { attrs: { 'data-icon': licon.LessThan, type: 'button' }, hook: bind('click', close) },
    name,
  );

export const elementScrollBarWidthSlowGuess: () => number = memoize<number>(() => {
  const ruler = document.createElement('div');
  ruler.style.position = 'absolute';
  ruler.style.overflow = 'scroll';
  ruler.style.visibility = 'hidden';
  document.body.appendChild(ruler);
  const barWidth = ruler.offsetWidth - ruler.clientWidth;
  document.body.removeChild(ruler);
  return barWidth;
});
