import * as licon from 'lib/licon';
import { bind, hl, type VNode } from 'lib/snabbdom';
import { memoize, Toggle } from 'lib';

export const header = (name: string, close: () => void): VNode =>
  hl(
    'button.head.text',
    { attrs: { 'data-icon': licon.LessThan, type: 'button' }, hook: bind('click', close) },
    name,
  );

export const moreButton = (toggle: Toggle): VNode =>
  hl(
    'button.button.more',
    {
      attrs: { title: i18n.site.more },
      hook: bind('click', toggle.toggle),
    },
    toggle() ? '-' : '+',
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
