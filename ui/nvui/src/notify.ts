import { requestIdleCallbackWithFallback } from 'common/common';
import { h, VNode } from 'snabbdom';

type Notification = {
  text: string;
  date: Date;
};

export class Notify {
  notification: Notification | undefined;

  constructor(
    readonly redraw: () => void,
    readonly timeout: number = 3000
  ) {}

  set = (msg: string): void => {
    // make sure it's different from previous, so it gets read again
    if (this.notification && this.notification.text == msg) msg += ' ';
    this.notification = { text: msg, date: new Date() };
    requestIdleCallbackWithFallback(this.redraw);
  };

  currentText = (): string =>
    this.notification && this.notification.date.getTime() > Date.now() - this.timeout ? this.notification.text : '';

  render = (): VNode =>
    h(
      'div.notify',
      {
        attrs: {
          'aria-live': 'assertive',
          'aria-atomic': 'true',
        },
      },
      this.currentText()
    );
}
