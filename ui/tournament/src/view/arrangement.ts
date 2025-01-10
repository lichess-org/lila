import { flatpickr } from 'common/assets';
import { bind } from 'common/snabbdom';
import statusView from 'game/view/status';
import { i18n } from 'i18n';
import { type VNode, type VNodes, h } from 'snabbdom';
import type TournamentController from '../ctrl';
import type { Arrangement, ArrangementUser } from '../interfaces';
import { backControl, utcControl } from './controls';
import header from './header';
import { adjustDateToLocal, adjustDateToUTC, formattedDate, player as renderPlayer } from './util';

export function arrangementView(ctrl: TournamentController, a: Arrangement): VNodes {
  return [header(ctrl), controls(ctrl), arrangement(ctrl, a)];
}

function controls(ctrl: TournamentController) {
  return backControl(
    ctrl,
    () => {
      ctrl.showArrangement(undefined);
    },
    [utcControl(ctrl)],
  );
}

function arrangement(ctrl: TournamentController, a: Arrangement): VNode {
  const hasMe = a.user1.id === ctrl.opts.userId || a.user2.id === ctrl.opts.userId,
    player1 = ctrl.data.standing.players.find(p => p.id === a.user1.id),
    player2 = ctrl.data.standing.players.find(p => p.id === a.user2.id),
    utc = ctrl.utc();
  console.log('arrangement', ctrl.data.standing.players, a, player1, player2);
  if (player1 && player2) {
    return h(
      'div.arrangement',
      {
        // key: JSON.stringify(a),
        class: {
          flipped: hasMe && a.user1.id === ctrl.opts.userId,
        },
      },
      [
        h(
          'div.arr-user.top',
          {
            class: {
              winner: a.winner === a.user1.id,
              loser: !!a.winner && a.winner !== a.user1.id,
            },
          },
          [
            renderPlayer(player1, true, true),
            hasMe && player1.id === ctrl.opts.userId
              ? myActions(ctrl, a, a.user1)
              : otherActions(a, a.user1, hasMe, utc),
          ],
        ),
        middleButtons(ctrl, a, hasMe, utc),
        h(
          'div.arr-user.bottom',
          {
            class: {
              winner: a.winner === a.user2.id,
              loser: !!a.winner && a.winner !== a.user2.id,
            },
          },
          [
            renderPlayer(player2, true, true),
            hasMe && player2.id === ctrl.opts.userId
              ? myActions(ctrl, a, a.user2)
              : otherActions(a, a.user2, hasMe, utc),
          ],
        ),
      ],
    );
  } else return h('div', 'Players not found');
}

let fInstance: any = null;
function myActions(ctrl: TournamentController, a: Arrangement, user: ArrangementUser): VNode {
  const disabled = !!a.gameId;
  return h('div.actions', [
    h('input.flatpickr', {
      attrs: {
        disabled,
        placeholder: !disabled ? 'Suggest time for game' : '',
        'data-enable-time': 'true',
        'data-time_24h': 'true',
      },
      hook: {
        insert: (node: VNode) => {
          flatpickr().then(() => {
            fInstance = window.flatpickr(node.elm as HTMLInputElement, {
              minDate: 'today',
              maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31 * 3),
              dateFormat: 'U',
              altInput: true,
              altFormat: 'Z',
              formatDate: (date, format) => {
                const utc = ctrl.utc();
                if (utc) date = adjustDateToLocal(date);

                if (format === 'U') return Math.floor(date.getTime()).toString();
                return formattedDate(date, utc);
              },
              parseDate: (dateString, format) => {
                console.log('parseDate', dateString, format, new Date(dateString));
                if (format === 'U') {
                  return new Date(Number.parseInt(dateString));
                }
                return new Date(dateString);
              },
              disableMobile: true,
              position: 'above center',
              locale: document.documentElement.lang as any,
            });
            console.log('fInstance: ', fInstance);
            if (user.scheduledAt) {
              const scheduledDate = new Date(user.scheduledAt),
                finalDate = ctrl.utc() ? adjustDateToUTC(scheduledDate) : scheduledDate;
              fInstance.setDate(finalDate, false);
              fInstance.altInput.value = formattedDate(scheduledDate, ctrl.utc());
            }
          });
        },
        destroy: () => {
          console.log('destroy:', fInstance);
          if (fInstance) fInstance.destroy();
          fInstance = null;
        },
        postpatch: () => {
          console.log('postpatch:', fInstance);
          if (fInstance && user.scheduledAt) {
            const utc = ctrl.utc(),
              scheduledDate = new Date(user.scheduledAt),
              finalDate = utc ? adjustDateToUTC(scheduledDate) : scheduledDate;
            fInstance.setDate(finalDate, false);
            fInstance.altInput.value = formattedDate(scheduledDate, utc);
          }
        },
      },
    }),
    h('div.user-button.confirm', {
      hook: bind('click', () => {
        ctrl.arrangementTime(a, fInstance.selectedDates[0]);
      }),
      class: {
        disabled: disabled,
        action: !!fInstance?.altInput.value && !user.scheduledAt,
      },
      attrs: { 'data-icon': 'E' },
    }),
    h('div.user-button.clear', {
      hook: bind('click', () => {
        ctrl.arrangementTime(a, undefined);
      }),
      class: {
        disabled: disabled || !fInstance?.altInput.value,
      },
      attrs: { 'data-icon': 'L' },
    }),
  ]);
}

function otherActions(a: Arrangement, u: ArrangementUser, hasMe: boolean, utc: boolean): VNode {
  const disabled =
    !!a.gameId ||
    !u.scheduledAt ||
    u.scheduledAt < new Date().getTime() ||
    a.scheduledAt === u.scheduledAt;
  return h('div.actions', [
    h('input.time-select', {
      attrs: {
        value: u.scheduledAt ? formattedDate(new Date(u.scheduledAt), utc) : '',
        disabled: true,
      },
    }),
    hasMe
      ? h(
          'div.user-button.confirm',
          { class: { disabled, active: !disabled && !!u.scheduledAt } },
          h('span', { attrs: { 'data-icon': 'E' } }),
        )
      : undefined,
    hasMe
      ? h('a.user-button.message', {
          attrs: {
            href: '/inbox/' + u.id,
            'data-icon': 'c',
          },
        })
      : null,
  ]);
}

function infoLine(title: string, value: string | number | undefined): VNode {
  return h('div.info-line', [h('div.title', title), h('div.value', value || '-')]);
}

function middleButtons(
  ctrl: TournamentController,
  a: Arrangement,
  hasMe: boolean,
  utc: boolean,
): VNode {
  const winner = a.winner
    ? ctrl.data.standing.players.find(p => p.id === a.winner)?.name
    : undefined;
  return h('div.arr-agreed', [
    h('div.infos', [
      infoLine(
        i18n('tourArrangements:scheduledAt'),
        a.scheduledAt ? formattedDate(new Date(a.scheduledAt), utc) : undefined,
      ),
      infoLine(i18n('search:result'), a.status ? statusView(a.status, a.color, false) : undefined),
      infoLine(i18n('winner'), winner),
      infoLine(i18n('nbMoves'), a.plies),
    ]),
    h('div.game-button', [
      h('div.timer'),
      a.gameId
        ? h('a.button', { attrs: { href: '/' + a.gameId } }, i18n('tourArrangements:goToGame'))
        : h(
            'button.button',
            {
              hook: bind('click', _ => ctrl.arrangementMatch(a, true)),
              attrs: { disabled: !hasMe },
            },
            i18n('tourArrangements:startGame'),
          ),
      !a.gameId ? h('div.warning', i18n('tourArrangements:gameWillNotStart')) : null,
    ]),
    h(
      'div.history',
      {
        key: a.history ? JSON.stringify(a.history[0]) : 'empty',
      },
      h(
        'div.history-inner',
        {
          hook: {
            insert: vnode => {
              const element = vnode.elm as HTMLElement;
              element.scrollTop = element.scrollHeight;
            },
          },
        },
        (a.history?.slice().reverse() || []).map(e => {
          const split = e.split(';');
          if (split.length !== 3) return null;
          const userId = split[0] === '1' ? a.user1.id : a.user2.id,
            player = ctrl.data.standing.players.find(p => p.id === userId);
          if (player) {
            return h('div.line', {
              hook: {
                insert: vnode => {
                  const date = formattedDate(
                    new Date(Number.parseInt(split[1]) * 1000),
                    ctrl.utc(),
                  );
                  (vnode.elm as HTMLElement).innerHTML =
                    `<span>${player.name}</span> ` +
                    historyAction(split[2]) +
                    ` <time>${date}</time>`;
                  vnode.data!.cachedUTC = ctrl.utc;
                },
                postpatch(old, vnode) {
                  if (old.data!.cachedUTC !== ctrl.utc) {
                    const date = formattedDate(
                      new Date(Number.parseInt(split[1]) * 1000),
                      ctrl.utc(),
                    );
                    (vnode.elm as HTMLElement).innerHTML =
                      `<span>${player.name}</span> ` +
                      historyAction(split[2]) +
                      ` <time>${date}</time>`;
                  }
                  vnode.data!.cachedUTC = ctrl.utc;
                },
              },
            });
          } else null;
        }),
      ),
    ),
  ]);
}

function historyAction(action: string) {
  switch (action) {
    case 'A':
      return i18n('tourArrangements:accepted');
    case 'M':
      return i18n('tourArrangements:removed');
    case 'P':
      return i18n('tourArrangements:proposed');
    case 'R':
      return i18n('tourArrangements:ready');
    case 'N':
      return i18n('tourArrangements:notReady');
    case 'S':
      return i18n('tourArrangements:starts');
    default:
      return '?';
  }
}
