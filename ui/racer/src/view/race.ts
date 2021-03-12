import { h } from 'snabbdom';
import RacerCtrl from '../ctrl';
import { Player, Race } from '../interfaces';

// to [0,1]
type RelativeMoves = (moves: number) => number;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.moves < m ? p.moves : m), 999) / 2;
  const maxMoves = players.reduce((m, p) => (p.moves > m ? p.moves : m), 30);
  const delta = maxMoves - minMoves;
  const relative: RelativeMoves = moves => (moves - minMoves) / delta;
  return h('div.racer__race', players.map(renderTrack(relative)));
};

const renderTrack = (relative: RelativeMoves) => (player: Player, index: number) =>
  h(
    'div.racer__race__track',
    h(
      'div.racer__race__player',
      {
        attrs: {
          style: `padding-left:${relative(player.moves) * 95}%`,
        },
      },
      [h(`div.racer__race__player__car.car-${index}`, [0]), h('span.racer__race__player__name', playerLink(player))]
    )
  );

export const playerLink = (player: Player) =>
  player.userId
    ? h(
        'a.user-link.ulpt',
        {
          attrs: { href: '/@/' + player.name },
        },
        player.title ? [h('span.utitle', player.title), player.name] : [player.name]
      )
    : h('anonymous', player.name);
