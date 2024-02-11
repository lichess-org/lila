import { looseH as h } from 'common/snabbdom';
import * as licon from 'common/licon';
import { Player } from 'game';
import { Position } from '../interfaces';
import RoundController from '../ctrl';
import { ratingDiff, userLink } from 'common/userLink';

export const aiName = (ctrl: RoundController, level: number) =>
  ctrl.trans('aiNameLevelAiLevel', 'Stockfish', level);

export function userHtml(ctrl: RoundController, player: Player, position: Position) {
  const d = ctrl.data,
    user = player.user,
    perf = (user?.perfs || {})[d.game.perf],
    rating = player.rating || perf?.rating,
    signal = user?.id === d.opponent.user?.id ? d.opponentSignal : undefined;

  if (user) {
    const connecting = !player.onGame && ctrl.firstSeconds && user.online;
    return h(
      `div.ruser-${position}.ruser.user-link`,
      {
        class: {
          online: player.onGame,
          offline: !player.onGame,
          long: user.username.length > 16,
          connecting,
        },
      },
      [
        h('i.line' + (user.patron ? '.patron' : ''), {
          attrs: {
            title: connecting
              ? 'Connecting to the game'
              : player.onGame
              ? 'Joined the game'
              : 'Left the game',
          },
        }),
        userLink({
          name: user.username,
          ...user,
          attrs: { 'data-pt-pos': 's', ...(ctrl.isPlaying() ? { target: '_blank', rel: 'noopener' } : {}) },
          online: false,
          line: false,
        }),
        !!signal && signalBars(signal),
        !!rating && h('rating', rating + (player.provisional ? '?' : '')),
        !!rating && ratingDiff(player),
        player.engine &&
          h('span', {
            attrs: { 'data-icon': licon.CautionCircle, title: ctrl.noarg('thisAccountViolatedTos') },
          }),
      ],
    );
  }
  const connecting = !player.onGame && ctrl.firstSeconds;
  return h(
    `div.ruser-${position}.ruser.user-link`,
    { class: { online: player.onGame, offline: !player.onGame, connecting } },
    [
      h('i.line', {
        attrs: {
          title: connecting ? 'Connecting to the game' : player.onGame ? 'Joined the game' : 'Left the game',
        },
      }),
      h('name', player.name || ctrl.noarg('anonymous')),
    ],
  );
}

const signalBars = (signal: number) => {
  const bars = [];
  for (let i = 1; i <= 4; i++) bars.push(h(i <= signal ? 'i' : 'i.off'));
  return h('signal.q' + signal, bars);
};

export const userTxt = (ctrl: RoundController, player: Player) =>
  player.user
    ? (player.user.title ? player.user.title + ' ' : '') + player.user.username
    : player.ai
    ? aiName(ctrl, player.ai)
    : ctrl.noarg('anonymous');
