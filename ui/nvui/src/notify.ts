import { h } from 'snabbdom';
import { requestIdleCallback } from 'common';

type Notification = {
  text: string;
  date: Date;
};

export class Notify {
  notification: Notification | undefined;
  redraw?: () => void;

  constructor(readonly timeout: number = 3000) {}

  set = (msg: string) => {
    // make sure it's different from previous, so it gets read again
    if (this.notification && this.notification.text == msg) msg += ' ';
    this.notification = { text: msg, date: new Date() };
    requestIdleCallback(() => this.redraw && this.redraw(), 500);
  };

  currentText = () =>
    this.notification && this.notification.date.getTime() > Date.now() - this.timeout
      ? this.notification.text
      : '';

  render = () =>
    h('div.notify', { attrs: { 'aria-live': 'assertive', 'aria-atomic': 'true' } }, this.currentText());
}
