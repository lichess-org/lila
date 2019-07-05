import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ModerationCtrl, ModerationOpts, ModerationData, ModerationReason } from './interfaces'
import { userModInfo } from './xhr'
import { userLink, spinner, bind } from './util';

export function moderationCtrl(opts: ModerationOpts): ModerationCtrl {

  let data: ModerationData | undefined;
  let loading = false;

  const open = (username: string) => {
    if (opts.permissions.timeout) {
      loading = true;
      userModInfo(username).then(d => {
        data = d;
        loading = false;
        opts.redraw();
      });
    } else {
      data = {
        id: username,
        username
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
    timeout(reason: ModerationReason) {
      data && window.lichess.pubsub.emit('socket.send', 'timeout', {
        userId: data.id,
        reason: reason.key
      });
      close();
      opts.redraw();
    },
    shadowban() {
      loading = true;
      data && $.post('/mod/' + data.id + '/troll/true').then(() => data && open(data.username));
      opts.redraw();
    }
  };
}

export function lineAction(username: string) {
  return h('i.mod', {
    attrs: {
      'data-icon': '',
      'data-username': username,
      title: 'Moderation'
    }
  });
}

export function moderationView(ctrl?: ModerationCtrl): VNode[] | undefined {
  if (!ctrl) return;
  if (ctrl.loading()) return [h('div.loading', spinner())];
  const data = ctrl.data();
  if (!data) return;
  const perms = ctrl.permissions();

  const infos = data.history ? h('div.infos.block', [
    window.lichess.numberFormat(data.games || 0) + ' games',
    data.troll ? 'TROLL' : undefined,
    data.engine ? 'ENGINE' : undefined,
    data.booster ? 'BOOSTER' : undefined
  ].map(t => t && h('span', t)).concat([
    h('a', {
      attrs: {
        href: '/@/' + data.username + '?mod'
      }
    }, 'profile')
  ]).concat(
    perms.shadowban ? [
      h('a', {
        attrs: {
          href: '/mod/' + data.username + '/communication'
        }
      }, 'coms')
    ] : [])) : undefined;

    const timeout = perms.timeout ? h('div.timeout.block', [
      h('strong', 'Timeout 10 minutes for'),
      ...ctrl.reasons.map(r => {
        return h('a.text', {
          attrs: { 'data-icon': 'p' },
          hook: bind('click', () => ctrl.timeout(r))
        }, r.name);
      }),
      ...(
        (data.troll || !perms.shadowban) ? [] : [h('div.shadowban', [
          'Or ',
          h('button.button.button-red.button-empty', {
            hook: bind('click', ctrl.shadowban)
          }, 'shadowban')
        ])])
    ]) : h('div.timeout.block', [
      h('strong', 'Moderation'),
      h('a.text', {
        attrs: { 'data-icon': 'p' },
        hook: bind('click', () => ctrl.timeout(ctrl.reasons[0]))
      }, 'Timeout 10 minutes')
    ]);

    const history = data.history ? h('div.history.block', [
      h('strong', 'Timeout history'),
      h('table', h('tbody.slist', {
        hook: {
          insert: () => window.lichess.pubsub.emit('content_loaded')
        }
      }, data.history.map(function(e) {
        return h('tr', [
          h('td.reason', e.reason),
          h('td.mod', e.mod),
          h('td', h('time.timeago', {
            attrs: { datetime: e.date }
          }))
        ]);
      })))
    ]) : undefined;

    return [
      h('div.top', { key: 'mod-' + data.id }, [
        h('span.text', {
          attrs: {'data-icon': '' },
        }, [userLink(data.username)]),
        h('a', {
          attrs: {'data-icon': 'L'},
          hook: bind('click', ctrl.close)
        })
      ]),
      h('div.mchat__content.moderation', [
        infos,
        timeout,
        history
      ])
    ];
};
