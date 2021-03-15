import config from '../config';
import RacerCtrl from '../ctrl';
import renderClock from 'puz/view/clock';
import { bind } from 'puz/util';
import { h } from 'snabbdom';
import { playModifiers, renderCombo } from 'puz/view/util';
import { renderRace } from './race';
import { VNode } from 'snabbdom/vnode';
import { MaybeVNodes } from 'puz/interfaces';
import { renderBoard } from './board';

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
  switch (ctrl.status()) {
    case 'pre':
      return ctrl.race.lobby
        ? [
            waitingToStart(),
            ctrl.vm.startsAt ? "It's racing time!" : 'Waiting for more players to join...',
            comboZone(ctrl),
          ]
        : [
            waitingToStart(),
            ctrl.raceFull() ? undefined : ctrl.isPlayer() ? renderLink(ctrl) : renderJoin(ctrl),
            comboZone(ctrl),
          ];
    case 'racing':
      if (ctrl.isPlayer())
        return ctrl.run.endAt
          ? [
              playerScore(ctrl),
              h('div.racer__end', [
                h('h2', 'Your time is up!'),
                h('div.race__end__players', playersInTheRace(ctrl)),
                ...(ctrl.race.lobby ? [newRaceForm(ctrl)] : [waitForRematch(), newRaceForm(ctrl)]),
              ]),
              comboZone(ctrl),
            ]
          : [playerScore(ctrl), renderClock(ctrl.run, ctrl.endNow, false), comboZone(ctrl)];
      return [
        spectating(),
        h('div.racer__spectating', [
          renderClock(ctrl.run, ctrl.endNow, false),
          ctrl.race.lobby ? newRaceForm(ctrl) : waitForRematch(),
        ]),
        comboZone(ctrl),
      ];
    case 'post':
      const nextRace = ctrl.race.lobby ? newRaceForm(ctrl) : rematchButton(ctrl);
      return ctrl.isPlayer()
        ? [
            playerScore(ctrl),
            h('div.racer__post', [h('h2', 'Race complete!'), yourRank(ctrl), nextRace]),
            comboZone(ctrl),
          ]
        : [spectating(), h('div.racer__post', [h('h2', 'Race complete!'), nextRace]), comboZone(ctrl)];
  }
};

const puzzleRacer = () => h('strong', 'Puzzle Racer');

const waitingToStart = () =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [puzzleRacer(), h('span', 'Waiting to start')])
  );

const spectating = () =>
  h('div.puz-side__top.puz-side__start', h('div.puz-side__start__text', [puzzleRacer(), h('span', 'Spectating')]));

const renderBonus = (bonus: number) => `+${bonus}`;

const comboZone = (ctrl: RacerCtrl) => h('div.puz-side__table', [renderCombo(config, renderBonus)(ctrl.run)]);

const playerScore = (ctrl: RacerCtrl): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', ctrl.myScore() || 0)]);

const playersInTheRace = (ctrl: RacerCtrl) =>
  h('div.race__players-racing', `${ctrl.players().filter(p => !p.end).length} players still in the race.`);

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
          'data-icon': '"',
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
      'Join the race!'
    )
  );

const yourRank = (ctrl: RacerCtrl) => {
  const score = ctrl.myScore();
  if (!score) return;
  const players = ctrl.players();
  const rank = players.filter(p => p.score > score).length + 1;
  return h('strong.race__post__rank', `Your rank: ${rank}/${players.length}`);
};

const waitForRematch = () =>
  h(
    `a.racer__new-race.button.button-fat.button-navaway.disabled`,
    {
      attrs: { disabled: true },
    },
    'Wait for rematch'
  );

const newRaceForm = (ctrl: RacerCtrl) =>
  h(
    'form',
    {
      attrs: {
        action: ctrl.race.lobby ? '/racer/lobby' : '/racer',
        method: 'post',
      },
    },
    [
      h(`button.racer__new-race.button.button-navaway${ctrl.race.lobby ? '.button-fat' : '.button-empty'}`, [
        ctrl.race.lobby ? 'Next race' : 'New race',
      ]),
    ]
  );

const rematchButton = (ctrl: RacerCtrl) =>
  h(
    `a.racer__rematch.button.button-fat.button-navaway`,
    {
      attrs: { href: `/racer/${ctrl.race.id}/rematch` },
    },
    'Join rematch'
  );
