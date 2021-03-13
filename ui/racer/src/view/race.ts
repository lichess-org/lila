import RacerCtrl from '../ctrl';
import { h } from 'snabbdom';
import { PlayerWithMoves } from '../interfaces';
import { Boost } from '../boost';

// to [0,1]
type RelativeMoves = (moves: number) => number;

const trackHeight = 30;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.moves < m ? p.moves : m), 999) / 3;
  const maxMoves = players.reduce((m, p) => (p.moves > m ? p.moves : m), 30);
  const delta = maxMoves - minMoves;
  const relative: RelativeMoves = moves => (moves - minMoves) / delta;
  const bestScore = players.reduce((b, p) => (p.moves > b ? p.moves : b), 0);
  return h(
    'div.racer__race',
    {
      attrs: {
        style: `height:${players.length * trackHeight + 30}px`,
      },
    },
    h('div.racer__race__tracks', players.map(renderTrack(relative, ctrl.player().name, bestScore, ctrl.boost)))
  );
};

const renderTrack = (relative: RelativeMoves, myName: string, bestScore: number, boost: Boost) => (
  player: PlayerWithMoves,
  index: number
) => {
  const isMe = player.name == myName;
  return h(
    'div.racer__race__track',
    {
      class: {
        'racer__race__track--me': isMe,
        'racer__race__track--first': player.moves && player.moves == bestScore,
        'racer__race__track--boost': boost.isBoosting(index),
        'racer__race__track--end': player.end,
      },
    },
    [
      h(
        'div.racer__race__player',
        {
          attrs: {
            style: `transform:translateX(${relative(player.moves) * 95}%)`,
          },
        },
        [
          h(`div.racer__race__player__car.car-${index}`, [0]),
          h('span.racer__race__player__name', playerLink(player, isMe)),
        ]
      ),
      h('div.racer__race__score', player.moves),
    ]
  );
};

export const playerLink = (player: PlayerWithMoves, isMe: boolean) =>
  player.userId
    ? h(
        'a.user-link.ulpt',
        {
          attrs: { href: '/@/' + player.name },
        },
        player.title ? [h('span.utitle', player.title), player.name] : [player.name]
      )
    : h('anonymous', [player.name, isMe ? ' (you)' : undefined]);
