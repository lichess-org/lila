import { h, type VNode, type VNodeData } from 'snabbdom';
import { requestIdleCallback } from '../common';
import { isMac } from '../device';

export class Notify {
  text = '';
  date: Date | undefined;

  constructor(public redraw: Redraw | undefined = undefined) {}

  set = (msg: string): void => {
    this.text = msg + (this.text === msg ? ' ' : '');
    this.date = new Date();

    requestIdleCallback(() => this.redraw?.(), 500);
  };

  render = (): VNode => liveText(this.text, 'assertive', 'div.notify', this.date);
}

export function liveText(
  text: string,
  live: 'assertive' | 'polite' = 'polite',
  sel: string = 'p',
  forceKey?: Date,
): VNode {
  const data: VNodeData = isMac()
    ? { key: forceKey?.getTime().toString() || text, attrs: { role: 'alert', 'tab-index': '-1' } }
    : { attrs: { 'aria-live': live, 'aria-atomic': 'true', 'tab-index': '-1' } };
  return h(sel, data, text);
}
