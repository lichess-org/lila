import { Ctrl, NotifyData, type Notification } from './interfaces';
import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import makeRenderers from './renderers';

export default function view(ctrl: Ctrl): VNode {
  const d = ctrl.data();
  return h(
    'div#notify-app.links.dropdown',
    d && !ctrl.initiating() ? renderContent(ctrl, d) : [h('div.initiating', spinner())],
  );
}

function renderContent(ctrl: Ctrl, d: NotifyData): VNode[] {
  const pager = d.pager;
  const nb = pager.currentPageResults.length;

  const nodes: VNode[] = [];

  nodes.push(
    h(`div.pager.prev${pager.previousPage ? '' : '.disabled'}`, {
      attrs: { 'data-icon': licon.UpTriangle },
      hook: clickHook(ctrl.previousPage),
    }),
  );

  if (nb > 0)
    nodes.push(
      h('button.delete.button.button-empty', {
        attrs: { 'data-icon': licon.Trash, title: 'Clear' },
        hook: clickHook(ctrl.clear),
      }),
    );

  nodes.push(nb ? recentNotifications(d, ctrl.scrolling()) : empty());

  if (pager.nextPage)
    nodes.push(
      h('div.pager.next', { attrs: { 'data-icon': licon.DownTriangle }, hook: clickHook(ctrl.nextPage) }),
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
    { attrs: { href: '/faq#browser-notifications', target: '_blank', rel: 'noopener' } },
    'Notification popups disabled by browser setting',
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

const contentLoaded = (vnode: VNode) => site.contentLoaded(vnode.elm as HTMLElement);

function recentNotifications(d: NotifyData, scrolling: boolean): VNode {
  const trans = site.trans(d.i18n);
  return h(
    'div',
    {
      class: { notifications: true, scrolling },
      hook: { insert: contentLoaded, postpatch: contentLoaded },
    },
    d.pager.currentPageResults.map(n => asHtml(n, trans)) as VNode[],
  );
}

function empty() {
  return h('div.empty.text', { attrs: { 'data-icon': licon.InfoCircle } }, 'No notifications.');
}
