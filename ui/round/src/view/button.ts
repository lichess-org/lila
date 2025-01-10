import { modal } from 'common/modal';
import { type MaybeVNode, type MaybeVNodes, onInsert } from 'common/snabbdom';
import spinner from 'common/spinner';
import { debounce } from 'common/timings';
import * as game from 'game';
import { game as gameRoute } from 'game/router';
import * as status from 'game/status';
import { i18n, i18nFormat, i18nVdomPlural } from 'i18n';
import { type Hooks, type VNode, h } from 'snabbdom';
import type RoundController from '../ctrl';
import type { RoundData } from '../interfaces';
import * as util from '../util';

function analysisBoardOrientation(data: RoundData) {
  return data.player.color;
}

function standardStudyForm(ctrl: RoundController): VNode {
  return h(
    'form',
    {
      attrs: {
        method: 'post',
        action: '/study/as',
      },
    },
    [
      h('input', {
        attrs: { type: 'hidden', name: 'gameId', value: ctrl.data.game.id },
      }),
      h('input', {
        attrs: { type: 'hidden', name: 'orientation', value: ctrl.shogiground.state.orientation },
      }),
      h(
        'button.button',
        {
          attrs: {
            type: 'submit',
          },
        },
        i18n('study:createStudy'),
      ),
    ],
  );
}

function postGameStudyForm(ctrl: RoundController): VNode {
  return h(
    'form',
    {
      hook: util.onInsert(el => {
        $(el).on('submit', e => {
          e.preventDefault();
          const formData = $(e.target).serialize();
          console.log(formData); // todo

          debounce(
            () => {
              window.lishogi.xhr
                .json('POST', '/study/post-game-study')
                .then(res => {
                  if (res.redirect) {
                    ctrl.setRedirecting();
                    window.lishogi.properReload = true;
                    window.location.href = res.redirect;
                  }
                })
                .catch(res => alert(`${res.statusText} - ${res.error}`));
            },
            1000,
            true,
          )();
        });
      }),
    },
    [
      h('input', {
        attrs: { type: 'hidden', name: 'gameId', value: ctrl.data.game.id },
      }),
      h('div', [
        h('label', i18n('studyWith')),
        h('input.user-invite', {
          hook: onInsert<HTMLInputElement>(el => {
            window.lishogi.userAutocomplete($(el), {
              tag: 'span',
              focus: true,
            });
          }),
          attrs: {
            name: 'invited',
            placeholder: i18n('study:searchByUsername') + ` (${i18n('optional').toLowerCase()})`,
          },
        }),
      ]),
      h('input', {
        attrs: { type: 'hidden', name: 'orientation', value: ctrl.shogiground.state.orientation },
      }),
      h(
        'button.button',
        {
          attrs: {
            type: 'submit',
          },
        },
        i18n('study:createStudy'),
      ),
    ],
  );
}

function studyAdvancedButton(ctrl: RoundController): VNode | null {
  const d = ctrl.data;
  return game.replayable(d) && !!ctrl.data.player.user
    ? h(
        'a.fbt.new-study-button',
        {
          hook: util.bind('click', _ => {
            ctrl.openStudyModal = true;
            ctrl.redraw();
          }),
        },
        '+',
      )
    : null;
}

function studyModal(ctrl: RoundController): VNode {
  const d = ctrl.data;
  return modal({
    class: 'study__invite',
    onClose() {
      ctrl.openStudyModal = false;
      ctrl.redraw();
    },
    content: [
      h('div', [
        h('div.study-option', [
          h('div.study-title', i18n('postGameStudy')),
          h('div.desc', i18n('postGameStudyExplanation')),
          postGameStudyForm(ctrl),
          h(
            'a.text',
            { attrs: { 'data-icon': '', href: `/study/post-game-study/${d.game.id}/hot` } },
            i18n('postGameStudiesOfGame'),
          ),
        ]),
        h('div.study-option', [
          h('div.study-title', i18n('standardStudy')),
          standardStudyForm(ctrl),
        ]),
      ]),
    ],
  });
}

let loadingStudy = false;
let initiatedStudy = false;
function studyButton(ctrl: RoundController): VNode | null {
  const d = ctrl.data,
    isAnon = !ctrl.data.player.user,
    withAnonOrAnon = !ctrl.data.opponent.user || isAnon;
  const title = withAnonOrAnon
    ? i18n('postGameStudy')
    : !!ctrl.data.player.spectator
      ? i18n('studyOfPlayers')
      : i18n('studyWithOpponent');
  return game.replayable(d)
    ? h('div.post-game-study', [
        ctrl.postGameStudyOffer && !loadingStudy
          ? h(
              'button.post-game-study-decline',
              {
                attrs: {
                  'data-icon': 'L',
                  title: i18n('decline'),
                },
                hook: util.bind('click', () => {
                  ctrl.postGameStudyOffer = false;
                  ctrl.redraw();
                }),
              },
              ctrl.nvui ? i18n('decline') : '',
            )
          : null,
        d.game.postGameStudy && !loadingStudy
          ? h(
              'a.fbt',
              {
                class: {
                  glowing: ctrl.postGameStudyOffer && !loadingStudy && !initiatedStudy,
                },
                attrs: { href: '/study/' + d.game.postGameStudy },
                hook: util.bind('click', () => {
                  ctrl.postGameStudyOffer = false;
                }),
              },
              h('span', title),
            )
          : h(
              'form',
              {
                attrs: {
                  method: 'post',
                  action: '/study/post-game-study/' + d.game.id,
                },
                hook: util.bind('submit', () => {
                  setTimeout(() => {
                    loadingStudy = false;
                    ctrl.redraw();
                  }, 2500);
                  loadingStudy = true;
                  initiatedStudy = true;
                  ctrl.redraw();
                }),
              },
              [
                h(
                  'button.fbt',
                  {
                    class: {
                      inactive: loadingStudy,
                    },
                    attrs: {
                      type: 'submit',
                      disabled: !!ctrl.data.player.spectator || isAnon,
                    },
                  },
                  loadingStudy ? spinner() : title,
                ),
              ],
            ),
        loadingStudy ? null : studyAdvancedButton(ctrl),
      ])
    : null;
}

function analysisButton(ctrl: RoundController): VNode | null {
  const d = ctrl.data,
    url = gameRoute(d, analysisBoardOrientation(d)) + '#' + ctrl.ply;
  return game.replayable(d)
    ? h(
        'a.fbt',
        {
          attrs: { href: url },
          hook: util.bind('click', _ => {
            // force page load in case the URL is the same
            if (location.pathname === url.split('#')[0]) location.reload();
          }),
        },
        i18n('analysis'),
      )
    : null;
}

function rematchButtons(ctrl: RoundController): MaybeVNodes {
  const d = ctrl.data,
    me = !!d.player.offeringRematch,
    them = !!d.opponent.offeringRematch;
  return [
    them
      ? h(
          'button.rematch-decline',
          {
            attrs: {
              'data-icon': 'L',
              title: i18n('decline'),
            },
            hook: util.bind('click', () => {
              ctrl.socket.send('rematch-no');
            }),
          },
          ctrl.nvui ? i18n('decline') : '',
        )
      : null,
    h(
      'button.fbt.rematch.sente',
      {
        class: {
          me,
          glowing: them,
          disabled: !me && !(d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user)),
        },
        attrs: {
          title: them
            ? i18n('yourOpponentWantsToPlayANewGameWithYou')
            : me
              ? i18n('rematchOfferSent')
              : '',
        },
        hook: util.bind(
          'click',
          e => {
            const d = ctrl.data;
            if (d.game.rematch) location.href = gameRoute(d.game.rematch, d.opponent.color);
            else if (d.player.offeringRematch) {
              d.player.offeringRematch = false;
              ctrl.socket.send('rematch-no');
            } else if (d.opponent.onGame) {
              d.player.offeringRematch = true;
              ctrl.socket.send('rematch-yes');
            } else if (!(e.target as HTMLElement).classList.contains('disabled'))
              ctrl.challengeRematch();
          },
          ctrl.redraw,
        ),
      },
      [me ? spinner() : h('span', i18n('rematch'))],
    ),
  ];
}

export function resume(ctrl: RoundController): MaybeVNode {
  if (!status.paused(ctrl.data)) return null;
  const d = ctrl.data,
    me = !!d.player.offeringResume,
    them = !!d.opponent.offeringResume;
  return h('div.resume-button', [
    them
      ? h(
          'button.resume-decline',
          {
            attrs: {
              'data-icon': 'L',
              title: i18n('decline'),
            },
            hook: util.bind('click', () => {
              ctrl.socket.send('resume-no');
            }),
          },
          ctrl.nvui ? i18n('decline') : '',
        )
      : null,
    h(
      'button.fbt.resume.sente',
      {
        class: {
          me,
          glowing: them,
          disabled: !me && !(d.opponent.onGame || (!d.clock && d.player.user && d.opponent.user)),
        },
        attrs: {
          title: them
            ? i18n('yourOpponentProposesResumption')
            : me
              ? i18n('resumptionOfferSent')
              : '',
        },
        hook: util.bind(
          'click',
          () => {
            const d = ctrl.data;
            if (d.player.offeringResume) {
              d.player.offeringResume = false;
              ctrl.socket.send('resume-no');
            } else if (d.opponent.onGame) {
              d.player.offeringResume = true;
              ctrl.socket.send('resume-yes');
            }
          },
          ctrl.redraw,
        ),
      },
      [me ? spinner() : h('span', them ? i18n('acceptResumption') : i18n('offerResumption'))],
    ),
  ]);
}

export function standard(
  ctrl: RoundController,
  condition: ((d: RoundData) => boolean) | undefined,
  icon: string,
  hint: string,
  socketMsg: string,
  onclick?: () => void,
): VNode {
  // disabled if condition callback is provided and is falsy
  const enabled = () => !condition || condition(ctrl.data);
  return h(
    'button.fbt.' + socketMsg,
    {
      attrs: {
        disabled: !enabled(),
        title: hint,
      },
      hook: util.bind('click', _ => {
        if (enabled()) onclick ? onclick() : ctrl.socket.sendLoading(socketMsg);
      }),
    },
    [h('span', ctrl.nvui ? [hint] : util.justIcon(icon))],
  );
}

export function impasse(ctrl: RoundController): MaybeVNode {
  return h(
    'button.fbt.impasse',
    {
      attrs: {
        title: i18n('impasse'),
        disabled: !['standard', 'annanshogi', 'checkshogi'].includes(ctrl.data.game.variant.key),
      },
      class: { active: ctrl.impasseHelp },
      hook: util.bind('click', _ => {
        ctrl.impasseHelp = !ctrl.impasseHelp;
        ctrl.redraw();
      }),
    },
    [h('span', ctrl.nvui ? [i18n('impasse')] : util.justIcon('&'))],
  );
}

export function opponentGone(ctrl: RoundController): MaybeVNode {
  const gone = ctrl.opponentGone();
  return gone === true
    ? h('div.suggestion', [
        h('p', { hook: onSuggestionHook }, i18n('opponentLeftChoices')),
        h(
          'button.button',
          {
            hook: util.bind('click', () => ctrl.socket.sendLoading('resign-force')),
          },
          i18n('forceResignation'),
        ),
        h(
          'button.button',
          {
            hook: util.bind('click', () => ctrl.socket.sendLoading('draw-force')),
          },
          i18n('forceDraw'),
        ),
      ])
    : gone
      ? h('div.suggestion', [
          h('p', i18nVdomPlural('opponentLeftCounter', gone, h('strong', '' + gone))),
        ])
      : null;
}

function actConfirm(
  f: (v: boolean) => void,
  transKey: string,
  title: string,
  icon: string,
  klass?: string,
): VNode {
  return h('div.act-confirm.' + transKey, [
    h('button.fbt.yes.' + (klass || ''), {
      attrs: { title: title, 'data-icon': icon },
      hook: util.bind('click', () => f(true)),
    }),
    h('button.fbt.no', {
      attrs: { title: i18n('cancel'), 'data-icon': 'L' },
      hook: util.bind('click', () => f(false)),
    }),
  ]);
}

export function resignConfirm(ctrl: RoundController): VNode {
  return actConfirm(ctrl.resign, 'resign', i18n('resign'), 'b');
}

export function drawConfirm(ctrl: RoundController): VNode {
  return actConfirm(ctrl.offerDraw, 'offerDraw', i18n('offerDraw'), '', 'draw-yes');
}

export function pauseConfirm(ctrl: RoundController): VNode {
  return actConfirm(
    ctrl.offerPause,
    'offerAdjournment',
    i18n('offerAdjournment'),
    'Z',
    'pause-yes',
  );
}

export function cancelDrawOffer(ctrl: RoundController): MaybeVNode {
  return ctrl.data.player.offeringDraw ? h('div.pending', [h('p', i18n('drawOfferSent'))]) : null;
}

export function cancelPauseOffer(ctrl: RoundController): MaybeVNode {
  return ctrl.data.player.offeringPause
    ? h('div.pending', [h('p', i18n('adjournmentOfferSent'))])
    : null;
}

export function cancelResumeOffer(ctrl: RoundController): MaybeVNode {
  return ctrl.data.player.offeringResume
    ? h('div.pending', [h('p', i18n('resumptionOfferSent'))])
    : null;
}

export function answerOpponentDrawOffer(ctrl: RoundController): MaybeVNode {
  return ctrl.data.opponent.offeringDraw
    ? h('div.negotiation.draw', [
        h('p', i18n('yourOpponentOffersADraw')),
        acceptButton(ctrl, 'draw-yes', () => ctrl.socket.sendLoading('draw-yes')),
        declineButton(ctrl, () => ctrl.socket.sendLoading('draw-no')),
      ])
    : null;
}

export function answerOpponentPauseOffer(ctrl: RoundController): MaybeVNode {
  return ctrl.data.opponent.offeringPause
    ? h('div.negotiation.pause', [
        h('p', i18n('yourOpponentOffersAnAdjournment')),
        acceptButton(ctrl, 'pause-yes', () => ctrl.socket.sendLoading('pause-yes')),
        declineButton(ctrl, () => ctrl.socket.sendLoading('pause-no')),
      ])
    : null;
}

export function cancelTakebackProposition(ctrl: RoundController): MaybeVNode {
  return ctrl.data.player.proposingTakeback
    ? h('div.pending', [
        h('p', i18n('takebackPropositionSent')),
        h(
          'button.button',
          {
            hook: util.bind('click', () => ctrl.socket.sendLoading('takeback-no')),
          },
          i18n('cancel'),
        ),
      ])
    : null;
}

function acceptButton(ctrl: RoundController, klass: string, action: () => void) {
  const text = i18n('accept');
  return ctrl.nvui
    ? h(
        'button.' + klass,
        {
          hook: util.bind('click', action),
        },
        text,
      )
    : h('a.accept', {
        attrs: {
          'data-icon': 'E',
          title: text,
        },
        hook: util.bind('click', action),
      });
}
function declineButton(
  ctrl: RoundController,
  action: () => void,
  text: string | undefined = undefined,
) {
  text = text || i18n('decline');
  return ctrl.nvui
    ? h(
        'button',
        {
          hook: util.bind('click', action),
        },
        text,
      )
    : h('a.decline', {
        attrs: {
          'data-icon': 'L',
          title: text,
        },
        hook: util.bind('click', action),
      });
}

export function answerOpponentTakebackProposition(ctrl: RoundController): MaybeVNode {
  return ctrl.data.opponent.proposingTakeback
    ? h('div.negotiation.takeback', [
        h('p', i18n('yourOpponentProposesATakeback')),
        acceptButton(ctrl, 'takeback-yes', ctrl.takebackYes),
        declineButton(ctrl, () => ctrl.socket.sendLoading('takeback-no')),
      ])
    : null;
}

export function submitUsi(ctrl: RoundController): VNode | undefined {
  return ctrl.usiToSubmit
    ? h('div.negotiation.move-confirm', [
        declineButton(ctrl, () => ctrl.submitUsi(false), i18n('cancel')),
        h('p', i18n('confirmMove')),
        acceptButton(ctrl, 'confirm-yes', () => ctrl.submitUsi(true)),
      ])
    : undefined;
}

export function backToTournament(ctrl: RoundController): VNode | undefined {
  const d = ctrl.data;
  return d.tournament?.running
    ? h('div.follow-up', [
        h(
          'a.text.fbt.strong.glowing',
          {
            attrs: {
              'data-icon': 'G',
              href: '/tournament/' + d.tournament.id,
            },
            hook: util.bind('click', ctrl.setRedirecting),
          },
          i18n('backToTournament'),
        ),
        h(
          'form',
          {
            attrs: {
              method: 'post',
              action: '/tournament/' + d.tournament.id + '/withdraw',
            },
          },
          [h('button.text.fbt.weak', util.justIcon('Z'), i18n('pause'))],
        ),
        analysisButton(ctrl),
      ])
    : undefined;
}

export function moretime(ctrl: RoundController): MaybeVNode {
  return game.moretimeable(ctrl.data)
    ? h('a.moretime', {
        attrs: {
          title: ctrl.data.clock
            ? i18nFormat('giveNbSeconds', ctrl.data.clock.moretime)
            : i18n('preferences:giveMoreTime'),
          'data-icon': 'O',
        },
        hook: util.bind('click', ctrl.socket.moreTime),
      })
    : null;
}

export function followUp(ctrl: RoundController): VNode {
  const d = ctrl.data,
    rematchable =
      !d.game.rematch &&
      (status.finished(d) || status.aborted(d)) &&
      !d.tournament &&
      !d.simul &&
      !d.game.boosted,
    newable = (status.finished(d) || status.aborted(d)) && d.game.source === 'lobby',
    rematchZone = ctrl.challengeRematched
      ? [
          h(
            'div.suggestion.text',
            {
              hook: onSuggestionHook,
            },
            i18n('rematchOfferSent'),
          ),
        ]
      : rematchable || d.game.rematch
        ? rematchButtons(ctrl)
        : [];
  return h('div.follow-up', [
    ...rematchZone,
    d.tournament
      ? h(
          'a.fbt',
          {
            attrs: { href: '/tournament/' + d.tournament.id },
          },
          i18n('viewTournament'),
        )
      : null,
    newable
      ? h(
          'a.fbt',
          {
            attrs: {
              href: '/?hook_like=' + d.game.id,
            },
          },
          i18n('newOpponent'),
        )
      : null,
    studyButton(ctrl),
    analysisButton(ctrl),
    ctrl.openStudyModal ? studyModal(ctrl) : null,
  ]);
}

export function watcherFollowUp(ctrl: RoundController): VNode | null {
  const d = ctrl.data,
    content = [
      d.game.rematch
        ? h(
            'a.fbt.text',
            {
              attrs: {
                'data-icon': 'v',
                href: `/${d.game.rematch}/${d.opponent.color}`,
              },
            },
            i18n('viewRematch'),
          )
        : null,
      d.tournament
        ? h(
            'a.fbt',
            {
              attrs: { href: '/tournament/' + d.tournament.id },
            },
            i18n('viewTournament'),
          )
        : null,
      studyButton(ctrl),
      analysisButton(ctrl),
      ctrl.openStudyModal ? studyModal(ctrl) : null,
    ];
  return content.find(x => !!x) ? h('div.follow-up', content) : null;
}

const onSuggestionHook: Hooks = util.onInsert(el =>
  window.lishogi.pubsub.emit('round.suggestion', el.textContent),
);
