import { Ctrl, Challenge, ChallengeData, ChallengeDirection, ChallengeUser, TimeControl } from './interfaces';
import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { opposite } from 'chessground/util';

export const loaded = (ctrl: Ctrl): VNode =>
  ctrl.redirecting()
    ? h('div#challenge-app.dropdown', h('div.initiating', spinner()))
    : h('div#challenge-app.links.dropdown.rendered', renderContent(ctrl));

export const loading = (): VNode =>
  h('div#challenge-app.links.dropdown.rendered', h('div.empty.loading', '-'));

function renderContent(ctrl: Ctrl): VNode[] {
  const d = ctrl.data();
  const nb = d.in.length + d.out.length;
  return nb ? [allChallenges(ctrl, d, nb)] : [empty()];
}

const userPowertips = (vnode: VNode) => lichess.powertip.manualUserIn(vnode.elm as HTMLElement);

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
    d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out'))),
  );
}

function challenge(ctrl: Ctrl, dir: ChallengeDirection) {
  return (c: Challenge) => {
    const fromPosition = c.variant.key == 'fromPosition';
    const origColor = c.color == 'random' ? (fromPosition ? c.finalColor : 'random') : c.finalColor;
    const myColor = dir == 'out' ? origColor : origColor == 'random' ? 'random' : opposite(origColor);
    return h(
      `div.challenge.${dir}.c-${c.id}`,
      {
        class: {
          declined: !!c.declined,
        },
      },
      [
        h('div.content', [
          h(`div.content__text#challenge-text-${c.id}`, [
            h('span.head', renderUser(dir === 'in' ? c.challenger : c.destUser, ctrl.showRatings)),
            h('span.desc', [
              h('span.is.color-icon.' + myColor),
              ' • ',
              [ctrl.trans()(c.rated ? 'rated' : 'casual'), timeControl(c.timeControl), c.variant.name].join(
                ' • ',
              ),
            ]),
          ]),
          h('i.perf', {
            attrs: { 'data-icon': c.perf.icon },
          }),
        ]),
        fromPosition
          ? h('div.position.mini-board.cg-wrap.is2d', {
              attrs: { 'data-state': `${c.initialFen},${myColor}` },
              hook: {
                insert(vnode) {
                  lichess.miniBoard.init(vnode.elm as HTMLElement);
                },
              },
            })
          : null,
        h('div.buttons', (dir === 'in' ? inButtons : outButtons)(ctrl, c)),
      ],
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
            'aria-describedby': `challenge-text-${c.id}`,
            'data-icon': licon.Checkmark,
            title: trans('accept'),
          },
          hook: onClick(ctrl.onRedirect),
        }),
      ],
    ),
    h('button.button.decline', {
      attrs: {
        type: 'submit',
        'data-icon': licon.X,
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
        h('option', { attrs: { value: key } }, key == 'generic' ? '' : name),
      ),
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
          'data-icon': licon.Eye,
          href: '/' + c.id,
          title: trans('viewInFullSize'),
        },
      }),
    ]),
    h('button.button.decline', {
      attrs: {
        'data-icon': licon.X,
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

function renderUser(u: ChallengeUser | undefined, showRatings: boolean): VNode {
  if (!u) return h('span', 'Open challenge');
  const rating = u.rating + (u.provisional ? '?' : '');
  return h(
    'a.ulpt.user-link',
    {
      attrs: { href: `/@/${u.name}`, 'data-pt-pos': 'w' },
      class: { online: !!u.online },
    },
    [
      h('i.line' + (u.patron ? '.patron' : '')),
      h('name', [
        u.title && h('span.utitle', u.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, u.title + ' '),
        u.name + (showRatings ? ' (' + rating + ') ' : ''),
      ]),
      h(
        'signal',
        u.lag === undefined
          ? []
          : [1, 2, 3, 4].map(i =>
              h('i', {
                class: { off: u.lag! < i },
              }),
            ),
      ),
    ],
  );
}

const empty = (): VNode =>
  h(
    'div.empty.text',
    {
      attrs: {
        'data-icon': licon.InfoCircle,
      },
    },
    'No challenges.',
  );

const onClick = (f: (e: Event) => void) => ({
  insert: (vnode: VNode) => {
    (vnode.elm as HTMLElement).addEventListener('click', f);
  },
});
