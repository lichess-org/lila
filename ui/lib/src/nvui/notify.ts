import { h, type VNode } from 'snabbdom';
import { requestIdleCallback } from '../common';

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
    const text = this.currentText();
    return h('div.notify', {
      key: text,
      attrs: { role: 'alert' }, // macos voiceover doesn't read aria-live on page load...
      hook: { insert: v => setTimeout(() => (v.elm!.textContent = text)) }, // so we do this
    });
  };
}
