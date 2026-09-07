import { hl, type VNode } from 'lib/view';

import { rtlMirroredIcons, type Icon } from '@/icons';

const iconClasses = (icon: Icon, classes: string[]): string[] => [
  ...(rtlMirroredIcons.has(icon) ? ['mirror-rtl'] : []),
  ...classes.filter(Boolean),
];

export function snabIcon(icon: Icon, ...classes: string[]): VNode {
  classes = iconClasses(icon, classes);
  return hl('span.svg-icon.', {
    class: {
      [`icon-${icon}`]: true,
      ...Object.fromEntries(classes.map(c => [c.replace(/^\./, ''), true])),
    },
    attrs: { 'aria-hidden': 'true' },
  });
}

export function domIcon(icon: Icon, ...classes: string[]): HTMLSpanElement {
  const element = document.createElement('span');
  element.classList.add('svg-icon', `icon-${icon}`, ...iconClasses(icon, classes));
  element.setAttribute('aria-hidden', 'true');
  return element;
}

export function htmlIcon(icon: Icon, ...classes: string[]): string {
  return $html`<span class="svg-icon icon-${icon} ${iconClasses(icon, classes).join(' ')}" aria-hidden="true"></span>`;
}
