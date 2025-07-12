import { h, type VNode, type VNodeData } from 'snabbdom';
import { requestIdleCallback } from '../common';
import { isMac } from '../device';

export class Notify {
  text = '';

  constructor(public redraw: Redraw | undefined = undefined) {}

  set = (msg: string): void => {
    this.text = msg;
    requestIdleCallback(() => {
      this.redraw?.();
      this.text = '';
    }, 500);
  };

  render = (): VNode => liveText(this.text, 'assertive', 'div.notify');
}

export function liveText(text: string, live: 'assertive' | 'polite' = 'polite', sel: string = 'p'): VNode {
  const data: VNodeData = isMac()
    ? { key: new Date().getTime().toString(), attrs: { role: 'alert' } }
    : { attrs: { 'aria-live': live, 'aria-atomic': 'true' } };
  return h(sel, data, text);
}
