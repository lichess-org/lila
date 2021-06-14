import { Ctrl, NotifyData, Notification } from './interfaces';
import { h, VNode } from 'snabbdom';
import makeRenderers from './renderers';

export default function view(ctrl: Ctrl): VNode {
  const d = ctrl.data();
  return h(
    'div#notify-app.links.dropdown',
    d && !ctrl.initiating() ? renderContent(ctrl, d) : [h('div.initiating', spinner())]
  );
}

function renderContent(ctrl: Ctrl, d: NotifyData): VNode[] {
  const pager = d.pager;
  const nb = pager.currentPageResults.length;

  const nodes: VNode[] = [];

  if (pager.previousPage)
    nodes.push(
      h('div.pager.prev', {
        attrs: { 'data-icon': '' },
        hook: clickHook(ctrl.previousPage),
      })
    );
  else if (pager.nextPage)
    nodes.push(
      h('div.pager.prev.disabled', {
        attrs: { 'data-icon': '' },
      })
    );

  nodes.push(nb ? recentNotifications(d, ctrl.scrolling()) : empty());

  if (pager.nextPage)
    nodes.push(
      h('div.pager.next', {
        attrs: { 'data-icon': '' },
        hook: clickHook(ctrl.nextPage),
      })
    );

  if (!('Notification' in window))
    nodes.push(h('div.browser-notification', 'Browser does not support notification popups'));
  else if (Notification.permission == 'denied') nodes.push(notificationDenied());

  return nodes;
}

export function asText(n: Notification, trans: Trans): string | undefined {
  const renderers = makeRenderers(trans);
  return renderers[n.type] ? renderers[n.type].text(n) : undefined;
}

function notificationDenied(): VNode {
  return h(
    'a.browser-notification.denied',
    {
      attrs: {
        href: '/faq#browser-notifications',
        target: '_blank',
      },
    },
    'Notification popups disabled by browser setting'
  );
}

function asHtml(n: Notification, trans: Trans): VNode | undefined {
  const renderers = makeRenderers(trans);
  return renderers[n.type] ? renderers[n.type].html(n) : undefined;
}

function clickHook(f: () => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener('click', f);
    },
  };
}

const contentLoaded = (vnode: VNode) => lichess.contentLoaded(vnode.elm as HTMLElement);

function recentNotifications(d: NotifyData, scrolling: boolean): VNode {
  const trans = lichess.trans(d.i18n);
  return h(
    'div',
    {
      class: {
        notifications: true,
        scrolling,
      },
      hook: {
        insert: contentLoaded,
        postpatch: contentLoaded,
      },
    },
    d.pager.currentPageResults.map(n => asHtml(n, trans)) as VNode[]
  );
}

function empty() {
  return h('div.empty.text', { attrs: { 'data-icon': '' } }, 'No notifications.');
}

function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [h('circle', { attrs: { cx: 20, cy: 20, r: 18, fill: 'none' } })]),
  ]);
}
