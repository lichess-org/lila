import { hl, type VNode } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import type { Player, TopOrBottom } from 'lib/game/game';
import type RoundController from '../ctrl';
import { ratingDiff, userLink } from 'lib/view/userLink';
import { wsAverageLag } from 'lib/socket';
import { defined } from 'lib';

export function userHtml(ctrl: RoundController, player: Player, position: TopOrBottom): VNode {
  const d = ctrl.data,
    user = player.user,
    perf = (user?.perfs || {})[d.game.perf],
    rating = player.rating || perf?.rating,
    showSignals = defined(d.opponentSignal) && defined(user?.id) && ctrl.isPlaying(),
    signal = showSignals
      ? user.id === d.opponent.user?.id
        ? d.opponentSignal
        : user.id === d.player.user?.id
          ? myWsLagAsSignal()
          : undefined
      : undefined;

  if (user) {
    const connecting = !player.onGame && ctrl.firstSeconds && user.online;
    return hl(
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
        hl('i.line' + (user.patron ? '.patron' : ''), {
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
          attrs: { 'data-pt-pos': 's', ...(ctrl.isPlaying() ? { target: '_blank' } : {}) },
          online: false,
          line: false,
        }),
        !!signal && signalBars(signal),
        !!rating && hl('rating', rating + (player.provisional ? '?' : '')),
        !!rating && ratingDiff(player),
        player.engine &&
          hl('span', {
            attrs: { 'data-icon': licon.CautionCircle, title: i18n.site.thisAccountViolatedTos },
          }),
      ],
    );
  }
  const connecting = !player.onGame && ctrl.firstSeconds;
  return hl(
    `div.ruser-${position}.ruser.user-link`,
    { class: { online: player.onGame, offline: !player.onGame, connecting } },
    [
      hl('i.line', {
        attrs: {
          title: connecting ? 'Connecting to the game' : player.onGame ? 'Joined the game' : 'Left the game',
        },
      }),
      hl('name', player.name || i18n.site.anonymous),
    ],
  );
}

const signalBars = (signal: number) => {
  const bars: VNode[] = [];
  for (let i = 1; i <= 4; i++) bars.push(hl(i <= signal ? 'i' : 'i.off'));
  return hl('signal.q' + signal, bars);
};

const myWsLagAsSignal = () => {
  const ping = wsAverageLag();
  return !ping ? 0 : ping < 150 ? 4 : ping < 300 ? 3 : ping < 500 ? 2 : 1;
};

export const userTxt = (player: Player): string =>
  player.user
    ? (player.user.title ? player.user.title + ' ' : '') + player.user.username
    : player.ai
      ? i18n.site.aiNameLevelAiLevel('Stockfish', player.ai)
      : i18n.site.anonymous;
