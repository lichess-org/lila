import { h, type VNode } from 'snabbdom';
import { requestIdleCallback } from '../common';
import { isApple } from '../device';

type Notification = {
  text: string;
  date: Date;
};

export class Notify {
  notification: Notification | undefined;
  redraw?: () => void;

  constructor() {}

  set = (msg: string): void => {
    // make sure it's different from previous, so it gets read again
    if (this.notification && this.notification.text === msg) msg += ' ';
    this.notification = { text: msg, date: new Date() };
    requestIdleCallback(() => this.redraw && this.redraw(), 500);
  };

  currentText = (): string =>
    this.notification && this.notification.date.getTime() > Date.now() - 3000 ? this.notification.text : '';

  render = (): VNode => {
    return h('div.notify', {
      key: this.currentText(),
      attrs: isApple() ? { role: 'alert' } : { 'aria-live': 'assertive', 'aria-atomic': 'true' },
      hook: { insert: v => setTimeout(() => (v.elm!.textContent = this.currentText()), 50) },
    });
  };
}

export function liveText(text: string, live: 'assertive' | 'polite' = 'polite', sel: string = 'p'): VNode {
  const liveAction = (vnode: VNode) => setTimeout(() => (vnode.elm!.textContent = text), 50);
  return h(sel, {
    key: text,
    attrs: isApple() ? { role: 'alert' } : { 'aria-live': live, 'aria-atomic': 'true' },
    hook: { insert: liveAction, update: liveAction },
  });
}
