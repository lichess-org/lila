import { numberFormat } from '@/i18n';
import { pubsub } from '@/pubsub';
import {
  type VNode,
  a,
  bind,
  button,
  snabH,
  confirm,
  dataIcon,
  div,
  onInsert,
  span,
  strong,
  table,
  tbody,
  td,
  tr,
  i,
} from '@/view';
import { userLink, profileUrl } from '@/view/userLink';

import { licon } from '../licon';
import type {
  ModerationCtrl,
  ModerationOpts,
  ModerationData,
  ModerationReason,
  ChatCtrl,
} from './interfaces';
import { userModInfo, flag, timeout } from './xhr';

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
        } else pubsub.emit('socket.send', 'timeout', body);
      }
      close();
      opts.redraw();
    },
  };
}

export function flagReport(ctrl: ChatCtrl, flag: HTMLElement): void {
  const line = flag.parentNode as HTMLElement;
  const text = flag.dataset['text'] as string;
  const userA = line.querySelector<HTMLLinkElement>('a.user-link');
  if (text && userA) reportUserText(ctrl.data.resourceId, userA.href.split('/')[4], text);
}
async function reportUserText(resourceId: string, username: string, text: string) {
  if (await confirm(`Report "${text}" to moderators?`)) flag(resourceId, username, text);
}

export const lineAction = (): VNode => snabH('action.mod', { attrs: dataIcon(licon.Agent) });

export function moderationView(ctrl?: ModerationCtrl): VNode[] | undefined {
  if (!ctrl) return undefined;
  if (ctrl.loading()) return [div('.loading')];
  const data = ctrl.data();
  if (!data) return undefined;
  const perms = ctrl.opts.permissions;

  const infos = data.history
    ? div(
        '.infos.block',
        [numberFormat(data.games || 0) + ' games', data.tos ? 'TOS' : undefined]
          .map(t => t && span(t))
          .concat([a(profileUrl(data.name) + '?mod')('profile')])
          .concat(perms.shadowban ? [a('/mod/' + data.name + '/communication')('coms')] : []),
      )
    : undefined;

  const timeout =
    perms.timeout || perms.broadcast
      ? div('.timeout.block', [
          strong('Timeout 15 minutes for'),
          ...ctrl.opts.reasons.map(r =>
            button(
              '.text',
              {
                attrs: dataIcon(licon.Clock),
                hook: bind('click', () => ctrl.timeout(r, data.text)),
              },
              r.name.split(';')[0],
            ),
          ),
        ])
      : div('.timeout.block', [
          strong('Moderation'),
          button(
            '.text',
            {
              attrs: dataIcon(licon.Clock),
              hook: bind('click', () => ctrl.timeout(ctrl.opts.reasons[0], data.text)),
            },
            'Timeout 15 minutes',
          ),
          button(
            '.text',
            {
              attrs: dataIcon(licon.Clock),
              hook: bind('click', async () => {
                await reportUserText(ctrl.opts.resourceId, data.name, data.text);
                ctrl.timeout(ctrl.opts.reasons[0], data.text);
              }),
            },
            'Timeout and report to Lichess',
          ),
        ]);

  const history = data.history
    ? div('.history.block', [
        strong('Timeout history'),
        table(
          tbody(
            '.slist',
            {
              hook: onInsert(() => pubsub.emit('content-loaded')),
            },
            data.history.map(function (e) {
              return tr([
                snabH('td.reason', e.reason),
                td('.mod', e.mod),
                td(snabH('time.timeago', { attrs: { datetime: e.date } })),
              ]);
            }),
          ),
        ),
      ])
    : undefined;

  return [
    div('.top', { key: 'mod-' + data.id }, [
      span('.text', { attrs: dataIcon(licon.Agent) }, [userLink(data)]),
      button({ attrs: dataIcon(licon.X), hook: bind('click', ctrl.close) }),
    ]),
    div('.mchat__content.moderation', [
      i('.line-text.block', ['"', data.text, '"']),
      infos,
      timeout,
      history,
    ]),
  ];
}
