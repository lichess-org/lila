import config from '../config';
import RacerCtrl from '../ctrl';
import renderClock from 'puz/view/clock';
import { bind } from 'puz/util';
import { h } from 'snabbdom';
import { playModifiers, renderCombo } from 'puz/view/util';
import { renderRace } from './race';
import { VNode } from 'snabbdom/vnode';
import { MaybeVNodes, Run } from 'puz/interfaces';
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
      return [
        waitingToStart(),
        ctrl.raceFull() ? undefined : ctrl.isPlayer() ? renderLink(ctrl) : renderJoin(ctrl),
        comboZone(ctrl),
      ];
    case 'racing':
      if (ctrl.isPlayer())
        return ctrl.run.endAt
          ? [
              playerScore(ctrl.run),
              h('div.racer__end', [
                h('h2', 'Your time is up!'),
                h('div.race__end__players', playersInTheRace(ctrl)),
                newRaceButton('.button-empty'),
              ]),
              comboZone(ctrl),
            ]
          : [playerScore(ctrl.run), renderClock(ctrl.run, ctrl.endNow), comboZone(ctrl)];
      return [
        spectating(),
        h('div.racer__spectating', [playersInTheRace(ctrl), newRaceButton('.button-empty')]),
        comboZone(ctrl),
      ];
    case 'post':
      return ctrl.isPlayer()
        ? [
            playerScore(ctrl.run),
            h('div.racer__post', [h('h2', 'Race complete!'), yourRank(ctrl), rematchButton(ctrl)]),
            comboZone(ctrl),
          ]
        : [spectating(), h('div.racer__post', [h('h2', 'Race complete!'), rematchButton(ctrl)]), comboZone(ctrl)];
  }
};

const puzzleRacer = () => h('strong', 'Puzzle Racer');

const waitingToStart = () =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [puzzleRacer(), h('span', 'Waiting to start')])
  );

const spectating = () =>
  h(
    'div.puz-side__top.puz-side__start',
    h('div.puz-side__start__text', [puzzleRacer(), h('span', 'Spectating the race')])
  );

const comboZone = (ctrl: RacerCtrl) => h('div.puz-side__table', [renderCombo(config)(ctrl.run)]);

const playerScore = (run: Run): VNode =>
  h('div.puz-side__top.puz-side__solved', [h('div.puz-side__solved__text', run.moves - run.errors)]);

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

const yourRank = (ctrl: RacerCtrl) =>
  h('strong.race__post__rank', `Your rank: ${ctrl.myRank()}/${ctrl.players().length}`);

const newRaceButton = (cls: string = '') =>
  h(
    `a.racer__new-race.button.button-navaway${cls}`,
    {
      attrs: { href: '/racer' },
    },
    'New race'
  );

const rematchButton = (ctrl: RacerCtrl) =>
  h(
    `a.racer__rematch.button.button-fat.button-navaway`,
    {
      attrs: { href: `/racer/${ctrl.race.id}/rematch` },
    },
    'Join rematch'
  );
