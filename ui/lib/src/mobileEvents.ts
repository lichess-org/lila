import { bind, type Hooks } from './view';

export const hookMobileMousedown = (f: (e: Event) => any): Hooks =>
  bind('ontouchstart' in window ? 'click' : 'mousedown', f);
