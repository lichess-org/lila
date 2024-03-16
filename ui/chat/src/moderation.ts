import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { bind } from 'common/snabbdom';
import { userLink } from 'common/userLink';
import { ModerationCtrl, ModerationOpts, ModerationData, ModerationReason } from './interfaces';
import { numberFormat } from 'common/number';
import { userModInfo, flag, timeout } from './xhr';
import ChatCtrl from './ctrl';

export function moderationCtrl(opts: ModerationOpts): ModerationCtrl {
  let data: ModerationData | undefined;
  let loading = false;

  const open = (line: HTMLElement) => {
    const userA = line.querySelector('a.user-link') as HTMLLinkElement;
    const text = (line.querySelector('t') as HTMLElement).innerText;
    const username = userA.href.split('/')[4];
    if (opts.permissions.timeout) {
      loading = true;
      userModInfo(username).then(d => {
        data = { ...d, text };
        loading = false;
        opts.redraw();
      });
    } else {
      data = { id: username.toLowerCase(), name: username, text };
    }
    opts.redraw();
  };

  const close = () => {
    data = undefined;
    loading = false;
    opts.redraw();
  };

  return {
    loading: () => loading,
    data: () => data,
    opts,
    open,
    close,
    async timeout(reason: ModerationReason, text: string) {
      if (data) {
        const body = { userId: data.id, reason: reason.key, text };
        if (new URLSearchParams(window.location.search).get('mod') === 'true') {
          await timeout(opts.resourceId, body);
          window.location.reload(); // to load new state since it won't be sent over the socket
        } else site.pubsub.emit('socket.send', 'timeout', body);
      }
      close();
      opts.redraw();
    },
  };
}

export function report(ctrl: ChatCtrl, line: HTMLElement) {
  const userA = line.querySelector('a.user-link') as HTMLLinkElement;
  const text = (line.querySelector('t') as HTMLElement).innerText;
  if (userA) reportUserText(ctrl.data.resourceId, userA.href.split('/')[4], text);
}
function reportUserText(resourceId: string, username: string, text: string) {
  if (confirm(`Report "${text}" to moderators?`)) flag(resourceId, username, text);
}

export const lineAction = () => h('action.mod', { attrs: { 'data-icon': licon.Agent } });

export function moderationView(ctrl?: ModerationCtrl): VNode[] | undefined {
  if (!ctrl) return;
  if (ctrl.loading()) return [h('div.loading')];
  const data = ctrl.data();
  if (!data) return;
  const perms = ctrl.opts.permissions;

  const infos = data.history
    ? h(
        'div.infos.block',
        [numberFormat(data.games || 0) + ' games', data.tos ? 'TOS' : undefined]
          .map(t => t && h('span', t))
          .concat([
            h(
              'a',
              {
                attrs: {
                  href: '/@/' + data.name + '?mod',
                },
              },
              'profile',
            ),
          ])
          .concat(
            perms.shadowban
              ? [
                  h(
                    'a',
                    {
                      attrs: {
                        href: '/mod/' + data.name + '/communication',
                      },
                    },
                    'coms',
                  ),
                ]
              : [],
          ),
      )
    : undefined;

  const timeout =
    perms.timeout || perms.broadcast
      ? h('div.timeout.block', [
          h('strong', 'Timeout 15 minutes for'),
          ...ctrl.opts.reasons.map(r =>
            h(
              'a.text',
              {
                attrs: { 'data-icon': licon.Clock },
                hook: bind('click', () => ctrl.timeout(r, data.text)),
              },
              r.name,
            ),
          ),
        ])
      : h('div.timeout.block', [
          h('strong', 'Moderation'),
          ...[
            h(
              'a.text',
              {
                attrs: { 'data-icon': licon.Clock },
                hook: bind('click', () => ctrl.timeout(ctrl.opts.reasons[0], data.text)),
              },
              'Timeout 15 minutes',
            ),
            h(
              'a.text',
              {
                attrs: { 'data-icon': licon.Clock },
                hook: bind('click', () => {
                  reportUserText(ctrl.opts.resourceId, data.name, data.text);
                  ctrl.timeout(ctrl.opts.reasons[0], data.text);
                }),
              },
              'Timeout and report to Lichess',
            ),
          ],
        ]);

  const history = data.history
    ? h('div.history.block', [
        h('strong', 'Timeout history'),
        h(
          'table',
          h(
            'tbody.slist',
            {
              hook: {
                insert() {
                  site.contentLoaded();
                },
              },
            },
            data.history.map(function (e) {
              return h('tr', [
                h('td.reason', e.reason),
                h('td.mod', e.mod),
                h('td', h('time.timeago', { attrs: { datetime: e.date } })),
              ]);
            }),
          ),
        ),
      ])
    : undefined;

  return [
    h('div.top', { key: 'mod-' + data.id }, [
      h('span.text', { attrs: { 'data-icon': licon.Agent } }, [userLink(data)]),
      h('a', { attrs: { 'data-icon': licon.X }, hook: bind('click', ctrl.close) }),
    ]),
    h('div.mchat__content.moderation', [
      h('i.line-text.block', ['"', data.text, '"']),
      infos,
      timeout,
      history,
    ]),
  ];
}
