import RacerCtrl from '../ctrl';
import { h } from 'snabbdom';
import { PlayerWithScore as PlayerWithScore } from '../interfaces';
import { Boost } from '../boost';

// to [0,1]
type RelativeScore = (score: number) => number;

const trackHeight = 30;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.score < m ? p.score : m), 999) / 3;
  const maxMoves = players.reduce((m, p) => (p.score > m ? p.score : m), 30);
  const delta = maxMoves - minMoves;
  const relative: RelativeScore = score => (score - minMoves) / delta;
  const bestScore = players.reduce((b, p) => (p.score > b ? p.score : b), 0);
  const myName = ctrl.player().name;
  const tracks = [];
  players.forEach((p, i) => {
    const isMe = p.name == myName;
    const track = renderTrack(relative, isMe, bestScore, ctrl.boost, p, i);
    if (isMe) tracks.unshift(track);
    else tracks.push(track);
  });
  return h(
    'div.racer__race',
    {
      attrs: {
        style: `height:${players.length * trackHeight + 30}px`,
      },
    },
    h('div.racer__race__tracks', tracks)
  );
};

const renderTrack = (
  relative: RelativeScore,
  isMe: boolean,
  bestScore: number,
  boost: Boost,
  player: PlayerWithScore,
  index: number
) => {
  return h(
    'div.racer__race__track',
    {
      class: {
        'racer__race__track--me': isMe,
        'racer__race__track--first': player.score && player.score == bestScore,
        'racer__race__track--boost': boost.isBoosting(index),
      },
    },
    [
      h(
        'div.racer__race__player',
        {
          attrs: {
            style: `transform:translateX(${relative(player.score) * 95}%)`,
          },
        },
        [
          h(`div.racer__race__player__car.car-${index}`, [0]),
          h('span.racer__race__player__name', playerLink(player, isMe)),
        ]
      ),
      h('div.racer__race__score', player.score),
    ]
  );
};

export const playerLink = (player: PlayerWithScore, isMe: boolean) =>
  player.userId
    ? h(
        'a.user-link.ulpt',
        {
          attrs: { href: '/@/' + player.name },
        },
        player.title ? [h('span.utitle', player.title), player.name] : [player.name]
      )
    : h(
        'anonymous',
        {
          attrs: {
            title: 'Anonymous player',
          },
        },
        [player.name, isMe ? ' (you)' : undefined]
      );
