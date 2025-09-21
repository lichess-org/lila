import type RacerCtrl from '../ctrl';
import { h, type VNodes } from 'snabbdom';
import type { PlayerWithScore } from '../interfaces';
import type { Boost } from '../boost';
import { userLink } from 'lib/view/userLink';

// to [0,1]
type RelativeScore = (score: number) => number;

const trackHeight = 25;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.score < m ? p.score : m), 130) / 3;
  const maxMoves = players.reduce((m, p) => (p.score > m ? p.score : m), 35);
  const delta = maxMoves - minMoves;
  const relative: RelativeScore = score => (score - minMoves) / delta;
  const bestScore = players.reduce((b, p) => (p.score > b ? p.score : b), 0);
  const myName = ctrl.player().name;
  const tracks: VNodes = [];
  players.forEach((p, i) => {
    const isMe = p.name === myName;
    const track = renderTrack(relative, isMe, bestScore, ctrl.boost, p, i, ctrl.vehicle[i]);
    if (isMe) tracks.unshift(track);
    else tracks.push(track);
  });
  return h(
    'div.racer__race',
    { attrs: { style: `height:${players.length * trackHeight + 14}px` } },
    h('div.racer__race__tracks', tracks),
  );
};

const renderTrack = (
  relative: RelativeScore,
  isMe: boolean,
  bestScore: number,
  boost: Boost,
  player: PlayerWithScore,
  index: number,
  vehicle: number,
) => {
  return h(
    'div.racer__race__track',
    {
      class: {
        'racer__race__track--me': isMe,
        'racer__race__track--first': !!player.score && player.score === bestScore,
        'racer__race__track--boost': boost.isBoosting(index),
      },
    },
    [
      h(
        'div.racer__race__player',
        {
          attrs: {
            style: `transform:translateX(${
              relative(player.score) * 95 * (document.dir === 'rtl' ? -1 : 1)
            }%)`,
          },
        },
        [
          h(`div.racer__race__player__car.car-${index}.vehicle${vehicle}`, [vehicle]),
          h('span.racer__race__player__name', playerLink(player, isMe)),
        ],
      ),
      h('div.racer__race__score', player.score),
    ],
  );
};

export const playerLink = (player: PlayerWithScore, isMe: boolean) =>
  player.id
    ? userLink({ ...player, line: false })
    : h('anonymous', { attrs: { title: 'Anonymous player' } }, [player.name, isMe ? ' (you)' : undefined]);
