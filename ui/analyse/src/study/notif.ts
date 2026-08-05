import { h } from 'snabbdom';

import type { MaybeVNode } from 'lib/view';

interface Notif {
  duration: number;
  text: string;
}

export class NotifCtrl {
  current?: Notif;
  timeoutId?: number;

  constructor(readonly redraw: () => void) {}

  set = (n: Notif) => {
    clearTimeout(this.timeoutId);
    this.current = n;
    this.timeoutId = setTimeout(() => {
      this.current = undefined;
      this.redraw();
    }, n.duration);
  };
  get = () => this.current;
}

export function view(ctrl: NotifCtrl): MaybeVNode {
  const c = ctrl.get();
  return c ? h('div.notif', c.text) : undefined;
}
