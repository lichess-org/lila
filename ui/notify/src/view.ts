import { pubsub } from 'lib/pubsub';
import { hl, type VNode, type LooseVNodes, spinnerVdom as spinner, onInsert, snabIcon } from 'lib/view';

import type { Ctrl, NotifyData, Notification } from './interfaces';
import makeRenderers from './renderers';

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
    hl('div.pager.prev', { class: { disabled: !pager.previousPage }, hook: clickHook(ctrl.previousPage) }, [
      snabIcon('upTriangle'),
    ]),
    hl(
      'a.settings.button.button-empty',
      { attrs: { href: '/account/preferences/notification', title: 'Notification Settings' } },
      [snabIcon('gear')],
    ),
    nb === 0
      ? empty()
      : [
          hl(
            'button.delete.button.button-empty',
            { attrs: { title: 'Clear' }, hook: clickHook(ctrl.clear) },
            [snabIcon('trash')],
          ),
          recentNotifications(d, ctrl.scrolling()),
        ],

    pager.nextPage && hl('div.pager.next', { hook: clickHook(ctrl.nextPage) }, [snabIcon('downTriangle')]),

    !('Notification' in window)
      ? hl('div.browser-notification', 'Browser does not support notification popups')
      : Notification.permission === 'denied' && notificationDenied(),
  ];
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
  return onInsert(el => {
    el.addEventListener('click', f);
  });
}

const contentLoaded = (vnode: VNode) => pubsub.emit('content-loaded', vnode.elm as HTMLElement);

function recentNotifications(d: NotifyData, scrolling: boolean): VNode {
  return hl(
    'div',
    {
      class: { notifications: true, scrolling },
      hook: { insert: contentLoaded, postpatch: contentLoaded },
    },
    d.pager.currentPageResults.map(asHtml),
  );
}

function empty() {
  return hl('div.empty.text', [snabIcon('infoCircle'), 'No notifications.']);
}
