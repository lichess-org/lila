import { hl, type VNode } from 'lib/view';

import type { Icon } from '@/icons';

export function snabIcon(icon: Icon, ...classes: string[]): VNode {
  classes = classes.filter(Boolean);
  return hl('span.svg-icon.', {
    class: { [`icon-${icon}`]: true, ...Object.fromEntries(classes.map(c => [c.replace(/^\./, ''), true])) },
    attrs: { 'aria-hidden': 'true' },
  });
}

export function domIcon(icon: Icon, ...classes: string[]): HTMLSpanElement {
  const element = document.createElement('span');
  element.classList.add('svg-icon', `icon-${icon}`, ...classes.filter(Boolean));
  element.setAttribute('aria-hidden', 'true');
  return element;
}

export function htmlIcon(icon: Icon, ...classes: string[]): string {
  return $html`<span class="svg-icon icon-${icon} ${classes.join(' ')}" aria-hidden="true"></span>`;
}
