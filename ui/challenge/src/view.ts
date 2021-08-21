import { Ctrl, Challenge, ChallengeData, ChallengeDirection, ChallengeUser, TimeControl } from './interfaces';
import { h, VNode } from 'snabbdom';
import spinner from 'common/spinner';

export function loaded(ctrl: Ctrl): VNode {
  return ctrl.redirecting()
    ? h('div#challenge-app.dropdown', h('div.initiating', spinner()))
    : h('div#challenge-app.links.dropdown.rendered', renderContent(ctrl));
}

export function loading(): VNode {
  return h('div#challenge-app.links.dropdown.rendered', [h('div.empty.loading', '-'), create()]);
}

function renderContent(ctrl: Ctrl): VNode[] {
  const d = ctrl.data();
  const nb = d.in.length + d.out.length;
  return nb ? [allChallenges(ctrl, d, nb)] : [empty(), create()];
}

function userPowertips(vnode: VNode) {
  lichess.powertip.manualUserIn(vnode.elm);
}

function allChallenges(ctrl: Ctrl, d: ChallengeData, nb: number): VNode {
  return h(
    'div.challenges',
    {
      class: { many: nb > 3 },
      hook: {
        insert: userPowertips,
        postpatch: userPowertips,
      },
    },
    d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out')))
  );
}

function challenge(ctrl: Ctrl, dir: ChallengeDirection) {
  return (c: Challenge) => {
    return h(
      'div.challenge.' + dir + '.c-' + c.id,
      {
        class: {
          declined: !!c.declined,
        },
      },
      [
        h('div.content', [
          h('span.head', renderUser(dir === 'in' ? c.challenger : c.destUser)),
          h('span.desc', [
            h('span.is.is2.color-icon.' + (c.color || 'random')),
            ' • ',
            [ctrl.trans()(c.rated ? 'rated' : 'casual'), timeControl(c.timeControl), c.variant.name].join(' • '),
          ]),
        ]),
        h('i', {
          attrs: { 'data-icon': c.perf.icon },
        }),
        h('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c)),
      ]
    );
  };
}

function inButtons(ctrl: Ctrl, c: Challenge): VNode[] {
  const trans = ctrl.trans();
  return [
    h(
      'form',
      {
        attrs: {
          method: 'post',
          action: `/challenge/${c.id}/accept`,
        },
      },
      [
        h('button.button.accept', {
          attrs: {
            type: 'submit',
            'data-icon': '',
            title: trans('accept'),
          },
          hook: onClick(ctrl.onRedirect),
        }),
      ]
    ),
    h('button.button.decline', {
      attrs: {
        type: 'submit',
        'data-icon': '',
        title: trans('decline'),
      },
      hook: onClick(() => ctrl.decline(c.id, 'generic')),
    }),
    h(
      'select.decline-reason',
      {
        hook: {
          insert: (vnode: VNode) => {
            const select = vnode.elm as HTMLSelectElement;
            select.addEventListener('change', () => ctrl.decline(c.id, select.value));
          },
        },
      },
      Object.entries(ctrl.reasons()).map(([key, name]) =>
        h('option', { attrs: { value: key } }, key == 'generic' ? '' : name)
      )
    ),
  ];
}

function outButtons(ctrl: Ctrl, c: Challenge) {
  const trans = ctrl.trans();
  return [
    h('div.owner', [
      h('span.waiting', ctrl.trans()('waiting')),
      h('a.view', {
        attrs: {
          'data-icon': '',
          href: '/' + c.id,
          title: trans('viewInFullSize'),
        },
      }),
    ]),
    h('button.button.decline', {
      attrs: {
        'data-icon': '',
        title: trans('cancel'),
      },
      hook: onClick(() => ctrl.cancel(c.id)),
    }),
  ];
}

function timeControl(c: TimeControl): string {
  switch (c.type) {
    case 'unlimited':
      return 'Unlimited';
    case 'correspondence':
      return c.daysPerTurn + ' days';
    case 'clock':
      return c.show || '-';
  }
}

function renderUser(u?: ChallengeUser): VNode {
  if (!u) return h('span', 'Open challenge');
  const rating = u.rating + (u.provisional ? '?' : '');
  return h(
    'a.ulpt.user-link',
    {
      attrs: { href: `/@/${u.name}` },
      class: { online: !!u.online },
    },
    [
      h('i.line' + (u.patron ? '.patron' : '')),
      h('name', [
        u.title && h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title + ' '),
        u.name + ' (' + rating + ') ',
      ]),
      h(
        'signal',
        u.lag === undefined
          ? []
          : [1, 2, 3, 4].map(i =>
              h('i', {
                class: { off: u.lag! < i },
              })
            )
      ),
    ]
  );
}

function create(): VNode {
  return h('a.create', {
    attrs: {
      href: '/?any#friend',
      'data-icon': '',
      title: 'Challenge someone',
    },
  });
}

function empty(): VNode {
  return h(
    'div.empty.text',
    {
      attrs: {
        'data-icon': '',
      },
    },
    'No challenges.'
  );
}

function onClick(f: (e: Event) => void) {
  return {
    insert: (vnode: VNode) => {
      (vnode.elm as HTMLElement).addEventListener('click', f);
    },
  };
}
