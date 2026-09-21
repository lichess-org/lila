import { h, type VNode, type VNodeData } from 'snabbdom';

import { pubsub } from '@/pubsub';

import { isMac } from '../device';
import { requestIdleCallbackSafe } from '../index';

export class Notify {
  text = '';
  date?: Date;

  constructor(public redraw: Redraw | undefined) {
    pubsub.on('socket.online', online => {
      this.set(online ? 'You are online' : 'You are disconnected');
    });
  }

  set = (msg: string): void => {
    this.text = msg + (this.text === msg ? '\u00A0' : '');
    this.date = new Date();

    requestIdleCallbackSafe(() => this.redraw?.(), 500);
  };

  render = (): VNode => liveText(this.text, 'assertive', 'div.notify', this.date);
}

export function liveText(
  text: string,
  live: 'assertive' | 'polite' = 'polite',
  sel = 'p',
  forceKey?: Date,
): VNode {
  const data: VNodeData = isMac()
    ? { key: forceKey?.getTime().toString() || text, attrs: { role: 'alert' } }
    : { attrs: { 'aria-live': live, 'aria-atomic': 'true' } };
  return h(sel, data, text);
}
