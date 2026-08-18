import { licon } from 'lib/licon';
import { povMessage } from 'lib/puz/run';
import renderClock from 'lib/puz/view/clock';
import renderHistory from 'lib/puz/view/history';
import { playModifiers, renderCombo } from 'lib/puz/view/util';
import { copyMeInput, type MaybeVNodes, bind, div, p, button, strong, span, a, form, h2 } from 'lib/view';

import config from '@/config';
import type RacerCtrl from '@/ctrl';

import { renderBoard } from './board';
import { renderRace } from './race';

export default function (ctrl: RacerCtrl) {
  return div(
    '.racer.racer-app.racer--play',
    { class: { ...playModifiers(ctrl.run), [`racer--${ctrl.status()}`]: true } },
    [
      renderBoard(ctrl),
      div('.puz-side', selectScreen(ctrl)),
      renderRace(ctrl),
      ctrl.status() === 'post' && ctrl.run.history.length > 0 ? renderHistory(ctrl) : null,
    ],
  );
}

const selectScreen = (ctrl: RacerCtrl): MaybeVNodes => {
  const combo = comboZone(ctrl);
  switch (ctrl.status()) {
    case 'pre': {
      const povMsg = p('.racer__pre__message__pov', povMessage(ctrl.run));
      return ctrl.race.lobby
        ? [
            waitingToStart,
            div('.racer__pre__message.racer__pre__message--with-skip', [
              div('.racer__pre__message__text', [
                p(
                  ctrl.knowsSkip()
                    ? i18n.storm[ctrl.vm.startsAt ? 'getReady' : 'waitingForMorePlayers']
                    : skipHelp,
                ),
                povMsg,
              ]),
              !ctrl.knowsSkip() ? renderSkip(ctrl) : null,
            ]),
            combo,
          ]
        : [
            waitingToStart,
            div('.racer__pre__message', [
              ctrl.raceFull()
                ? ctrl.isPlayer()
                  ? [renderStart(ctrl)]
                  : null
                : ctrl.isPlayer()
                  ? [renderLink(ctrl), renderStart(ctrl)]
                  : [renderJoin(ctrl)],
              povMsg,
            ]),
            combo,
          ];
    }
    case 'racing': {
      const clock = renderClock(ctrl.run, ctrl.end, false);
      return ctrl.isPlayer()
        ? [playerScore(ctrl), div('.puz-clock', [clock, renderSkip(ctrl)]), combo]
        : [
            spectating,
            div('.racer__spectating', [
              div('.puz-clock', clock),
              ctrl.race.lobby ? lobbyNext(ctrl) : waitForRematch,
            ]),
            combo,
          ];
    }
    case 'post': {
      const nextRace = ctrl.race.lobby ? lobbyNext(ctrl) : friendNext(ctrl);
      const raceComplete = h2(i18n.storm.raceComplete);
      return ctrl.isPlayer()
        ? [playerScore(ctrl), div('.racer__post', [raceComplete, yourRank(ctrl), nextRace]), combo]
        : [spectating, div('.racer__post', [raceComplete, nextRace]), combo];
    }
    default:
      return [];
  }
};

const renderSkip = (ctrl: RacerCtrl) =>
  button(
    '.racer__skip.button.button-red',
    {
      class: { disabled: !ctrl.canSkip() },
      title: i18n.storm.skipExplanation,
      hook: bind('click', ctrl.skip),
    },
    i18n.storm.skip,
  );

const skipHelp = p(i18n.storm.skipHelp);
const puzzleRacer = strong('Puzzle Racer');

const waitingToStart = div(
  '.puz-side__top.puz-side__start',
  div('.puz-side__start__text', [puzzleRacer, span(i18n.storm.waitingToStart)]),
);

const spectating = div(
  '.puz-side__top.puz-side__start',
  div('.puz-side__start__text', [puzzleRacer, span(i18n.storm.spectating)]),
);

const renderBonus = (bonus: number) => `+${bonus}`;

const renderControls = (ctrl: RacerCtrl) =>
  div(
    '.puz-side__control',
    button('.puz-side__control__flip.button', {
      class: { active: ctrl.flipped, 'button-empty': !ctrl.flipped },
      'data-icon': licon.ChasingArrows,
      title: i18n.site.flipBoard + ' (Keyboard: f)',
      hook: bind('click', ctrl.flip),
    }),
  );

const comboZone = (ctrl: RacerCtrl) =>
  div('.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(ctrl.run)]);

const playerScore = ({ myScore }: RacerCtrl) =>
  div('.puz-side__top.puz-side__solved', [div('.puz-side__solved__text', `${myScore() || 0}`)]);

const renderLink = ({ race }: RacerCtrl) =>
  div('.puz-side__link', [
    p(i18n.site.toInviteSomeoneToPlayGiveThisUrl),
    copyMeInput(`${window.location.protocol}//${window.location.host}/racer/${race.id}`, {
      inputAttrs: { readonly: true },
    }),
  ]);

const renderStart = (ctrl: RacerCtrl) => {
  if (!ctrl.isOwner() || ctrl.vm.startsAt) return null;
  return div(
    '.puz-side__start',
    button(
      '.button.button-fat',
      {
        class: { disabled: ctrl.players().length < 2 },
        hook: bind('click', ctrl.start),
        disabled: ctrl.players().length < 2,
      },
      i18n.storm.startTheRace,
    ),
  );
};

const renderJoin = (ctrl: RacerCtrl) =>
  div(
    '.puz-side__join',
    button('.button.button-fat', { hook: bind('click', ctrl.join) }, i18n.storm.joinTheRace),
  );

const yourRank = (ctrl: RacerCtrl) => {
  const score = ctrl.myScore();
  if (!score) return undefined;
  const players = ctrl.players();
  const rank = players.filter(p => p.score > score).length + 1;
  return strong('.race__post__rank', i18n.storm.yourRankX(`${rank}/${players.length}`));
};

const waitForRematch = button(
  '.racer__new-race.button.button-fat.button-navaway.disabled',
  { disabled: true },
  i18n.storm.waitForRematch,
);

const lobbyNext = ({ race }: RacerCtrl) =>
  form({ action: '/racer/lobby', method: 'post' }, [
    button(
      `.racer__new-race.button.button-navaway${race.lobby ? '.button-fat' : '.button-empty'}`,
      i18n.storm.nextRace,
    ),
  ]);

const friendNext = ({ race }: RacerCtrl) =>
  div('.racer__post__next', [
    a(`/racer/${race.id}/rematch`)(
      `.racer__rematch.button.button-fat.button-navaway`,
      i18n.storm.joinRematch,
    ),
    form(
      '.racer__post__next__new',
      { action: '/racer', method: 'post' },
      button('.racer__post__next__button.button.button-empty', { type: 'submit' }, i18n.storm.createNewGame),
    ),
  ]);
