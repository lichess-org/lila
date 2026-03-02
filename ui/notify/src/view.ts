import type { Ctrl, NotifyData, Notification } from './interfaces';
import { hl, type VNode, type LooseVNodes, spinnerVdom as spinner } from 'lib/view';
import * as licon from 'lib/licon';
import makeRenderers from './renderers';
import { pubsub } from 'lib/pubsub';

const renderers = makeRenderers();

export default function view(ctrl: Ctrl): VNode {
  const d = ctrl.data();
  return hl(
    'div#notify-app.links.dropdown',
    d && !ctrl.initiating() ? renderContent(ctrl, d) : [hl('div.initiating', spinner())],
  );
}

function renderContent(ctrl: Ctrl, d: NotifyData): LooseVNodes {
  const pager = d.pager;
  const nb = pager.currentPageResults.length;
  return [
    hl('div.pager.prev', {
      attrs: { 'data-icon': licon.UpTriangle },
      class: { disabled: !pager.previousPage },
      hook: clickHook(ctrl.previousPage),
    }),
    hl('a.settings.button.button-empty', {
      attrs: {
        href: '/account/preferences/notification',
        'data-icon': licon.Gear,
        title: 'Notification Settings',
      },
    }),
    nb === 0
      ? empty()
      : [
          hl('button.delete.button.button-empty', {
            attrs: { 'data-icon': licon.Trash, title: 'Clear' },
            hook: clickHook(ctrl.clear),
          }),
          recentNotifications(d, ctrl.scrolling()),
        ],

    pager.nextPage &&
      hl('div.pager.next', { attrs: { 'data-icon': licon.DownTriangle }, hook: clickHook(ctrl.nextPage) }),

    !('Notification' in window)
      ? hl('div.browser-notification', 'Browser does not support notification popups')
      : Notification.permission === 'denied' && notificationDenied(),
  ];
}

export function asText(n: Notification): string | undefined {
  return renderers[n.type] ? renderers[n.type].text(n) : undefined;
}

function notificationDenied(): VNode {
  return hl(
    'a.browser-notification.denied',
    { attrs: { href: '/faq#browser-notifications', target: '_blank' } },
    'Notification popups disabled by browser setting',
  );
}

function asHtml(n: Notification): VNode | undefined {
  return renderers[n.type] ? renderers[n.type].html(n) : undefined;
}

function clickHook(f: () => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener('click', f);
    },
  };
}

const contentLoaded = (vnode: VNode) => pubsub.emit('content-loaded', vnode.elm as HTMLElement);

function recentNotifications(d: NotifyData, scrolling: boolean): VNode {
  return hl(
    'div',
    {
      class: { notifications: true, scrolling },
      hook: { insert: contentLoaded, postpatch: contentLoaded },
    },
    d.pager.currentPageResults.map(n => asHtml(n)) as VNode[],
  );
}

function empty() {
  return hl('div.empty.text', { attrs: { 'data-icon': licon.InfoCircle } }, 'No notifications.');
}
