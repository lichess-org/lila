import { h, type VNode, type VNodeData } from 'snabbdom';

import { pubsub } from '@/pubsub';

import { isMac } from '../device';
import { requestIdleCallbackSafe } from '../index';

const announced = new Map<string, string>();

const ariaNotifier =
  'ariaNotify' in document &&
  ((text: string) => requestAnimationFrame(() => (document as any).ariaNotify(text)));

export class Notify {
  text = '';

  constructor(public redraw: Redraw | undefined) {
    pubsub.on('socket.online', online => {
      this.set(online ? 'You are online' : 'You are disconnected');
    });
  }

  set = (msg: string): void => {
    announced.set('div.notify', '');
    this.text = msg + (this.text === msg ? '\u00A0' : '');

    requestIdleCallbackSafe(() => this.redraw?.(), 500);
  };

  render = (): VNode => liveText(this.text, 'assertive', 'div.notify');
}

export function liveText(text: string, live: 'assertive' | 'polite' = 'polite', sel = 'p'): VNode {
  const data: VNodeData = {};
  const speakNow = text !== announced.get(sel);
  if (ariaNotifier) {
    if (speakNow) ariaNotifier(text);
  } else if (isMac()) {
    data.key = speakNow ? Date.now().toString() : text;
    data.attrs = { role: 'alert' };
  } else data.attrs = { 'aria-live': live, 'aria-atomic': 'true' };
  announced.set(sel, text);
  return h(sel, data, text);
}