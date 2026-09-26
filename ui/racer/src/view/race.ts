import { div, makeExoticTag, span, type VNode } from 'lib/view';
import { userLink } from 'lib/view/userLink';

import type RacerCtrl from '@/ctrl';
import type { PlayerWithScore } from '@/interfaces';

// to [0,1]
type RelativeScore = (score: number) => number;

const TRACK_HEIGHT = 25;

export const renderRace = (ctrl: RacerCtrl) => {
  const players = ctrl.players();
  const minMoves = players.reduce((m, p) => (p.score < m ? p.score : m), 130) / 3;
  const maxMoves = players.reduce((m, p) => (p.score > m ? p.score : m), 35);
  const delta = maxMoves - minMoves;
  const relative: RelativeScore = score => (score - minMoves) / delta;
  const bestScore = players.reduce((b, p) => (p.score > b ? p.score : b), 0);
  const myName = ctrl.player().name;
  const tracks: VNode[] = [];
  players.forEach((p, i) => {
    const isMe = p.name === myName;
    const track = renderTrack(relative, isMe, bestScore, ctrl, p, i);
    if (isMe) tracks.unshift(track);
    else tracks.push(track);
  });
  return div(
    '.racer__race',
    { style: { height: `${players.length * TRACK_HEIGHT + 14}px` } },
    div('.racer__race__tracks', tracks),
  );
};

const renderTrack = (
  relative: RelativeScore,
  isMe: boolean,
  bestScore: number,
  ctrl: RacerCtrl,
  player: PlayerWithScore,
  index: number,
) => {
  return div(
    '.racer__race__track',
    {
      class: {
        'racer__race__track--me': isMe,
        'racer__race__track--first': !!player.score && player.score === bestScore,
        'racer__race__track--boost': ctrl.boost.isBoosting(index),
      },
    },
    [
      div(
        '.racer__race__player',
        {
          style: {
            transform: `translateX(${relative(player.score) * 95 * (document.dir === 'rtl' ? -1 : 1)}%)`,
          },
        },
        [
          div(`.racer__race__player__car.car-${index}.vehicle${ctrl.vehicle[index]}`, ctrl.vehicle[index]),
          span('.racer__race__player__name', playerLink(player, isMe)),
        ],
      ),
      div('.racer__race__score', player.score),
    ],
  );
};

const anonymous = makeExoticTag('anonymous', { title: 'Anonymous player' });

export const playerLink = (player: PlayerWithScore, isMe: boolean) =>
  player.id ? userLink({ ...player, line: false }) : anonymous([player.name, isMe ? ' (you)' : undefined]);
