import { h, type VNode, type VNodeData } from 'snabbdom';

import { pubsub } from '@/pubsub';

import { isMac } from '../device';
import { requestIdleCallbackSafe } from '../index';

let announced = '';

const ariaNotifier =
  'ariaNotify' in document &&
  ((text: string) => text !== announced && requestAnimationFrame(() => (document as any).ariaNotify(text)));

export class Notify {
  text = '';

  constructor(public redraw: Redraw | undefined) {
    pubsub.on('socket.online', online => {
      this.set(online ? 'You are online' : 'You are disconnected');
    });
  }

  set = (msg: string): void => {
    announced = '';
    this.text = msg + (this.text === msg ? '\u00A0' : '');

    requestIdleCallbackSafe(() => this.redraw?.(), 500);
  };

  render = (): VNode => liveText(this.text, 'assertive', 'div.notify');
}

export function liveText(text: string, live: 'assertive' | 'polite' = 'polite', sel = 'p'): VNode {
  const data: VNodeData = {};
  if (!text) return h(sel, data, '');
  if (ariaNotifier) ariaNotifier(text);
  else if (isMac()) {
    data.key = announced !== text ? Date.now().toString() : text;
    data.attrs = { role: 'alert' };
  } else data.attrs = { 'aria-live': live, 'aria-atomic': 'true' };
  announced = text;
  return h(sel, data, text);
}
