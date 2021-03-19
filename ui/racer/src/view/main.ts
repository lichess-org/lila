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
  switch (ctrl.status()) {
    case 'pre':
      const povMsg = h('p.racer__pre__message__pov', ctrl.trans(povMessage(ctrl.run)));
      return ctrl.race.lobby
        ? [
            waitingToStart(),
            h('div.racer__pre__message', [
              h('p', ctrl.vm.startsAt ? "It's racing time!" : 'Waiting for more players to join...'),
              povMsg,
            ]),
            comboZone(ctrl),
          ]
        : [
            waitingToStart(),
            h('div.racer__pre__message', [
              h('p', ctrl.raceFull() ? undefined : ctrl.isPlayer() ? renderLink(ctrl) : renderJoin(ctrl)),
              povMsg,
            ]),
            comboZone(ctrl),
          ];
    case 'racing':
      const clock = renderClock(ctrl.run, ctrl.end, ctrl.trans, false);
      return ctrl.isPlayer()
        ? [playerScore(ctrl), clock, comboZone(ctrl)]
        : [
            spectating(),
            h('div.racer__spectating', [clock, ctrl.race.lobby ? newRaceForm(ctrl) : waitForRematch()]),
            comboZone(ctrl),
          ];
    case 'post':
      const nextRace = ctrl.race.lobby ? newRaceForm(ctrl) : friendNext(ctrl);
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

const friendNext = (ctrl: RacerCtrl) =>
  h('div.racer__post__next', [
    h(
      `a.racer__rematch.button.button-fat.button-navaway`,
      {
        attrs: { href: `/racer/${ctrl.race.id}/rematch` },
      },
      'Join rematch'
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
        'Create new race'
      )
    ),
  ]);
