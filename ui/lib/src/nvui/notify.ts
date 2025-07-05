import { h, type VNode, type VNodeData } from 'snabbdom';
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

  render = (): VNode => liveText(this.currentText(), 'assertive', 'div.notify');
}

export function liveText(text: string, live: 'assertive' | 'polite' = 'polite', sel: string = 'p'): VNode {
  const data: VNodeData = isApple()
    ? {
        attrs: { role: 'alert' },
        hook: { insert: (vnode: VNode) => setTimeout(() => (vnode.elm!.textContent = text), 50) },
      }
    : { attrs: { 'aria-live': live, 'aria-atomic': 'true' } };
  return h(sel, data, isApple() ? undefined : text);
}
