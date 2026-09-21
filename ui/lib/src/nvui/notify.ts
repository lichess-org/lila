import { h, type VNode, type VNodeData } from 'snabbdom';

import { isMac } from '../device';
import { requestIdleCallbackSafe } from '../index';

export class Notify {
  text = '';
  date?: Date;

  constructor(public redraw: Redraw | undefined) {
    startOfflineObserver(this, 10);
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

function startOfflineObserver(notify: Notify, offlineIntervalSeconds: number) {
  let isConnected = document.body.classList.contains('online');
  let offlineInterval: ReturnType<typeof setInterval> | null = null;
  let reconnectAttempt = 0;

  function startOfflineNotifications() {
    if (offlineInterval !== null) {
      return;
    }

    notify.set('You are disconnected');
    offlineInterval = setInterval(() => {
      if (!document.body.classList.contains('online')) {
        reconnectAttempt++;
        notify.set('You are disconnected. Reconnection attempt ' + reconnectAttempt);
      } else {
        stopOfflineNotifications();
      }
    }, offlineIntervalSeconds * 1000);
  }

  function stopOfflineNotifications() {
    if (offlineInterval !== null) {
      notify.set('You are online');
      clearInterval(offlineInterval);
      offlineInterval = null;
    }
  }

  const observer = new MutationObserver(mutations => {
    for (const mutation of mutations) {
      if (mutation.type !== 'attributes' || mutation.attributeName !== 'class') {
        continue;
      }

      const nowConnected = document.body.classList.contains('online');

      if (!nowConnected && isConnected) {
        isConnected = false;
        startOfflineNotifications();
      }

      if (nowConnected && !isConnected) {
        reconnectAttempt = 0;
        isConnected = true;
        stopOfflineNotifications();
      }
    }
  });

  observer.observe(document.body, {
    attributes: true,
    attributeFilter: ['class'],
    attributeOldValue: true,
  });
}
