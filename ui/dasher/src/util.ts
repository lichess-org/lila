import { memoize, type Toggle } from 'lib';
import { icons } from 'lib/icons';
import { bind, hl, snabIcon, type VNode } from 'lib/view';

export const header = (name: string, close: () => void): VNode =>
  hl('button.head.text', { attrs: { type: 'button' }, hook: bind('click', close) }, [
    snabIcon(icons.LessThan),
    name,
  ]);

export const moreButton = (toggle: Toggle): VNode =>
  hl(
    'button.button.more',
    {
      attrs: { title: toggle() ? i18n.site.less : i18n.site.more },
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
