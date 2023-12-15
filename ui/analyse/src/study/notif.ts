import { h, VNode } from 'snabbdom';

interface Notif {
  duration: number;
  text: string;
}

export class NotifCtrl {
  current: Notif | undefined;
  timeout: number;
  constructor(readonly redraw: () => void) {}
  set = (n: Notif) => {
    clearTimeout(this.timeout);
    this.current = n;
    this.timeout = setTimeout(() => {
      this.current = undefined;
      this.redraw();
    }, n.duration);
  };
  get = () => this.current;
}

export function view(ctrl: NotifCtrl): VNode | undefined {
  const c = ctrl.get();
  return c ? h('div.notif', c.text) : undefined;
}
