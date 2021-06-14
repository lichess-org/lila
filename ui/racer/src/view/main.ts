import config from '../config';
import RacerCtrl from '../ctrl';
import renderClock from 'puz/view/clock';
import { bind } from 'puz/util';
import { h, VNode } from 'snabbdom';
import { playModifiers, renderCombo } from 'puz/view/util';
import { renderRace } from './race';
import { MaybeVNodes } from 'puz/interfaces';
import { renderBoard } from './board';
import { povMessage } from 'puz/run';

export default function (ctrl: RacerCtrl): VNode {
  return h(
    'div.racer.racer-app.racer--play',
    {
      class: {
        ...playModifiers(ctrl.run),
        [`racer--${ctrl.status()}`]: true,
      },
    },
    [renderRace(ctrl), renderBoard(ctrl), h('div.puz-side', selectScreen(ctrl))]
  );
}

const selectScreen = (ctrl: RacerCtrl): MaybeVNodes => {
  const noarg = ctrl.trans.noarg;
  switch (ctrl.status()) {
    case 'pre': {
      const povMsg = h('p.racer__pre__message__pov', ctrl.trans(povMessage(ctrl.run)));
      return ctrl.race.lobby
        ? [
            waitingToStart(noarg),
            h('div.racer__pre__message.racer__pre__message--with-skip', [
              h('div.racer__pre__message__text', [
                h(
                  'p',
                  ctrl.knowsSkip() ? noarg(ctrl.vm.startsAt ? 'getReady' : 'waitingForMorePlayers') : skipHelp(noarg)
                ),
                povMsg,
              ]),
              ctrl.knowsSkip() ? null : renderSkip(ctrl),
            ]),
            comboZone(ctrl),
          ]
        : [
            waitingToStart(noarg),
            h('div.racer__pre__message', [
              ctrl.raceFull() ? undefined : ctrl.isPlayer() ? renderLink(ctrl) : renderJoin(ctrl),
              povMsg,
            ]),
            comboZone(ctrl),
          ];
    }
    case 'racing': {
      const clock = renderClock(ctrl.run, ctrl.end, false);
      return ctrl.isPlayer()
        ? [playerScore(ctrl), h('div.puz-clock', [clock, renderSkip(ctrl)]), comboZone(ctrl)]
        : [
            spectating(noarg),
            h('div.racer__spectating', [
              h('div.puz-clock', clock),
              ctrl.race.lobby ? lobbyNext(ctrl) : waitForRematch(noarg),
            ]),
            comboZone(ctrl),
          ];
    }
    case 'post': {
      const nextRace = ctrl.race.lobby ? lobbyNext(ctrl) : friendNext(ctrl);
      const raceComplete = h('h2', noarg('raceComplete'));
      return ctrl.isPlayer()
        ? [playerScore(ctrl), h('div.racer__post', [raceComplete, yourRank(ctrl), nextRace]), comboZone(ctrl)]
        : [spectating(noarg), h('div.racer__post', [raceComplete, nextRace]), comboZone(ctrl)];
    }
  }
};

const renderSkip = (ctrl: RacerCtrl) =>
  h(
    'button.racer__skip.button.button-red',
    {
      class: {
        disabled: !ctrl.canSkip(),
      },
      attrs: {
        title: ctrl.trans.noarg('skipExplanation'),
      },
      hook: bind('click', ctrl.skip),
    },
    ctrl.trans.noarg('skip')
  );

const skipHelp = (noarg: TransNoArg) => h('p', noarg('skipHelp'));

const puzzleRacer = () => h('strong', 'Puzzle Racer');

const waitingToStart = (noarg: TransNoArg) =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [puzzleRacer(), h('span', noarg('waitingToStart'))])
  );

const spectating = (noarg: TransNoArg) =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [puzzleRacer(), h('span', noarg('spectating'))])
  );

const renderBonus = (bonus: number) => `+${bonus}`;

const renderControls = (ctrl: RacerCtrl): VNode =>
  h(
    'div.puz-side__control',
    h('a.puz-side__control__flip.button', {
      class: {
        active: ctrl.flipped,
        'button-empty': !ctrl.flipped,
      },
      attrs: {
        'data-icon': '',
        title: ctrl.trans.noarg('flipBoard') + ' (Keyboard: f)',
      },
      hook: bind('click', ctrl.flip),
    })
  );

const comboZone = (ctrl: RacerCtrl) =>
  h('div.puz-side__table', [renderControls(ctrl), renderCombo(config, renderBonus)(ctrl.run)]);

const playerScore = (ctrl: RacerCtrl): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', ctrl.myScore() || 0)]);

const renderLink = (ctrl: RacerCtrl) =>
  h('div.puz-side__link', [
    h('p', ctrl.trans.noarg('toInviteSomeoneToPlayGiveThisUrl')),
    h('div', [
      h(`input#racer-url-${ctrl.race.id}.copyable.autoselect`, {
        attrs: {
          spellcheck: false,
          readonly: 'readonly',
          value: `${window.location.protocol}//${window.location.host}/racer/${ctrl.race.id}`,
        },
      }),
      h('button.copy.button', {
        attrs: {
          title: 'Copy URL',
          'data-rel': `racer-url-${ctrl.race.id}`,
          'data-icon': '',
        },
      }),
    ]),
  ]);

const renderJoin = (ctrl: RacerCtrl) =>
  h(
    'div.puz-side__join',
    h(
      'button.button.button-fat',
      {
        hook: bind('click', ctrl.join),
      },
      ctrl.trans.noarg('joinTheRace')
    )
  );

const yourRank = (ctrl: RacerCtrl) => {
  const score = ctrl.myScore();
  if (!score) return;
  const players = ctrl.players();
  const rank = players.filter(p => p.score > score).length + 1;
  return h('strong.race__post__rank', ctrl.trans('yourRankX', `${rank}/${players.length}`));
};

const waitForRematch = (noarg: TransNoArg) =>
  h(
    `a.racer__new-race.button.button-fat.button-navaway.disabled`,
    {
      attrs: { disabled: true },
    },
    noarg('waitForRematch')
  );

const lobbyNext = (ctrl: RacerCtrl) =>
  h(
    'form',
    {
      attrs: {
        action: '/racer/lobby',
        method: 'post',
      },
    },
    [
      h(
        `button.racer__new-race.button.button-navaway${ctrl.race.lobby ? '.button-fat' : '.button-empty'}`,
        ctrl.trans.noarg('nextRace')
      ),
    ]
  );

const friendNext = (ctrl: RacerCtrl) =>
  h('div.racer__post__next', [
    h(
      `a.racer__rematch.button.button-fat.button-navaway`,
      {
        attrs: { href: `/racer/${ctrl.race.id}/rematch` },
      },
      ctrl.trans.noarg('joinRematch')
    ),
    h(
      'form.racer__post__next__new',
      {
        attrs: {
          action: '/racer',
          method: 'post',
        },
      },
      h(
        'button.racer__post__next__button.button.button-empty',
        {
          attrs: {
            type: 'submit',
          },
        },
        ctrl.trans.noarg('createNewGame')
      )
    ),
  ]);
