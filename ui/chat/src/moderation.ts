import { h } from 'snabbdom';
import { ModerationCtrl, ModerationOpts, ModerationData, ModerationReason } from './interfaces';
import { numberFormat } from 'common/number';
import { userLink, bind } from './util';
import { userModInfo } from './xhr';
import { VNode } from 'snabbdom';

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
      data = {
        id: username.toLowerCase(),
        username,
        text,
      };
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
    reasons: opts.reasons,
    permissions: () => opts.permissions,
    open,
    close,
    timeout(reason: ModerationReason, text: string) {
      data &&
        lichess.pubsub.emit('socket.send', 'timeout', {
          userId: data.id,
          reason: reason.key,
          text,
        });
      close();
      opts.redraw();
    },
  };
}

export const lineAction = () => h('i.mod', { attrs: { 'data-icon': '' } });

export function moderationView(ctrl?: ModerationCtrl): VNode[] | undefined {
  if (!ctrl) return;
  if (ctrl.loading()) return [h('div.loading')];
  const data = ctrl.data();
  if (!data) return;
  const perms = ctrl.permissions();

  const infos = data.history
    ? h(
        'div.infos.block',
        [
          numberFormat(data.games || 0) + ' games',
          data.troll ? 'TROLL' : undefined,
          data.engine ? 'ENGINE' : undefined,
          data.booster ? 'BOOSTER' : undefined,
        ]
          .map(t => t && h('span', t))
          .concat([
            h(
              'a',
              {
                attrs: {
                  href: '/@/' + data.username + '?mod',
                },
              },
              'profile'
            ),
          ])
          .concat(
            perms.shadowban
              ? [
                  h(
                    'a',
                    {
                      attrs: {
                        href: '/mod/' + data.username + '/communication',
                      },
                    },
                    'coms'
                  ),
                ]
              : []
          )
      )
    : undefined;

  const timeout = perms.timeout
    ? h('div.timeout.block', [
        h('strong', 'Timeout 15 minutes for'),
        ...ctrl.reasons.map(r => {
          return h(
            'a.text',
            {
              attrs: { 'data-icon': 'p' },
              hook: bind('click', () => ctrl.timeout(r, data.text)),
            },
            r.name
          );
        }),
      ])
    : h('div.timeout.block', [
        h('strong', 'Moderation'),
        h(
          'a.text',
          {
            attrs: { 'data-icon': 'p' },
            hook: bind('click', () => ctrl.timeout(ctrl.reasons[0], data.text)),
          },
          'Timeout 15 minutes'
        ),
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
                  lichess.contentLoaded();
                },
              },
            },
            data.history.map(function (e) {
              return h('tr', [
                h('td.reason', e.reason),
                h('td.mod', e.mod),
                h(
                  'td',
                  h('time.timeago', {
                    attrs: { datetime: e.date },
                  })
                ),
              ]);
            })
          )
        ),
      ])
    : undefined;

  return [
    h('div.top', { key: 'mod-' + data.id }, [
      h(
        'span.text',
        {
          attrs: { 'data-icon': '' },
        },
        [userLink(data.username)]
      ),
      h('a', {
        attrs: { 'data-icon': 'L' },
        hook: bind('click', ctrl.close),
      }),
    ]),
    h('div.mchat__content.moderation', [h('i.line-text.block', ['"', data.text, '"']), infos, timeout, history]),
  ];
}
