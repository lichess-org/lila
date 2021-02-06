import { h } from 'snabbdom';

type Notification = {
  text: string;
  date: Date;
};

export class Notify {
  notification: Notification | undefined;

  constructor(readonly redraw: () => void, readonly timeout: number = 3000) {}

  set = (msg: string) => {
    // make sure it's different from previous, so it gets read again
    if (this.notification && this.notification.text == msg) msg += ' ';
    this.notification = { text: msg, date: new Date() };
    lichess.requestIdleCallback(this.redraw, 500);
  };

  currentText = () =>
    this.notification && this.notification.date.getTime() > Date.now() - this.timeout ? this.notification.text : '';

  render = () =>
    h(
      'div.notify',
      {
        attrs: {
          'aria-live': 'assertive',
          'aria-atomic': true,
        },
      },
      this.currentText()
    );
}
