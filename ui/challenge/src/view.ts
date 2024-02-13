import { Challenge, ChallengeData, ChallengeDirection, ChallengeUser, TimeControl } from './interfaces';
import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { spinnerVdom as spinner } from 'common/spinner';
import { userLink } from 'common/userLink';
import { opposite } from 'chessground/util';
import ChallengeCtrl from './ctrl';

export const loaded = (ctrl: ChallengeCtrl): VNode =>
  ctrl.redirecting
    ? h('div#challenge-app.dropdown', h('div.initiating', spinner()))
    : h('div#challenge-app.links.dropdown.rendered', renderContent(ctrl));

export const loading = (): VNode =>
  h('div#challenge-app.links.dropdown.rendered', h('div.empty.loading', '-'));

function renderContent(ctrl: ChallengeCtrl): VNode[] {
  const d = ctrl.data;
  const nb = d.in.length + d.out.length;
  return nb ? [allChallenges(ctrl, d, nb)] : [empty()];
}

const userPowertips = (vnode: VNode) => lichess.powertip.manualUserIn(vnode.elm as HTMLElement);

const allChallenges = (ctrl: ChallengeCtrl, d: ChallengeData, nb: number): VNode =>
  h(
    'div.challenges',
    {
      class: { many: nb > 3 },
      hook: { insert: userPowertips, postpatch: userPowertips },
    },
    d.in.map(challenge(ctrl, 'in')).concat(d.out.map(challenge(ctrl, 'out'))),
  );

function challenge(ctrl: ChallengeCtrl, dir: ChallengeDirection) {
  return (c: Challenge) => {
    const fromPosition = c.variant.key == 'fromPosition';
    const origColor = c.color == 'random' ? (fromPosition ? c.finalColor : 'random') : c.finalColor;
    const myColor = dir == 'out' ? origColor : origColor == 'random' ? 'random' : opposite(origColor);
    const opponent = dir === 'in' ? c.challenger : c.destUser;
    return h(
      `div.challenge.${dir}.c-${c.id}`,
      {
        class: { declined: !!c.declined },
      },
      [
        h('div.content', [
          h('div.content__text', { attrs: { id: `challenge-text-${c.id}` } }, [
            h('span.head', [renderUser(opponent, ctrl.showRatings), renderLag(opponent)]),
            h('span.desc', [
              h('span.is.color-icon.' + myColor),
              ' • ',
              [ctrl.trans(c.rated ? 'rated' : 'casual'), timeControl(c.timeControl), c.variant.name].join(
                ' • ',
              ),
            ]),
          ]),
          h('i.perf', { attrs: { 'data-icon': c.perf.icon } }),
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

function inButtons(ctrl: ChallengeCtrl, c: Challenge): VNode[] {
  return [
    h('form', { attrs: { method: 'post', action: `/challenge/${c.id}/accept` } }, [
      h('button.button.accept', {
        attrs: {
          type: 'submit',
          'aria-describedby': `challenge-text-${c.id}`,
          'data-icon': licon.Checkmark,
          title: ctrl.trans('accept'),
        },
        hook: onClick(ctrl.onRedirect),
      }),
    ]),
    h('button.button.decline', {
      attrs: { type: 'submit', 'data-icon': licon.X, title: ctrl.trans('decline') },
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
      Object.entries(ctrl.reasons).map(([key, name]) =>
        h('option', { attrs: { value: key } }, key == 'generic' ? '' : name),
      ),
    ),
  ];
}

const outButtons = (ctrl: ChallengeCtrl, c: Challenge) => [
  h('div.owner', [
    h('span.waiting', ctrl.trans('waiting')),
    h('a.view', {
      attrs: { 'data-icon': licon.Eye, href: '/' + c.id, title: ctrl.trans('viewInFullSize') },
    }),
  ]),
  h('button.button.decline', {
    attrs: { 'data-icon': licon.X, title: ctrl.trans('cancel') },
    hook: onClick(() => ctrl.cancel(c.id)),
  }),
];

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

const renderUser = (u: ChallengeUser | undefined, showRating: boolean): VNode =>
  u
    ? userLink({ ...u, line: true, rating: showRating ? u.rating : undefined, attrs: { 'data-pt-pos': 'w' } })
    : h('span', 'Open challenge');

const renderLag = (u?: ChallengeUser) =>
  u && h('signal', u.lag === undefined ? [] : [1, 2, 3, 4].map(i => h('i', { class: { off: u.lag! < i } })));

const empty = (): VNode =>
  h('div.empty.text', { attrs: { 'data-icon': licon.InfoCircle } }, 'No challenges.');

const onClick = (f: (e: Event) => void) => ({
  insert: (vnode: VNode) => {
    (vnode.elm as HTMLElement).addEventListener('click', f);
  },
});
