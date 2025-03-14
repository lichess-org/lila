import { looseH as h, type VNode } from 'common/snabbdom';
import * as licon from 'common/licon';
import type { Player } from 'game';
import type { Position } from '../interfaces';
import type RoundController from '../ctrl';
import { ratingDiff, userLink } from 'common/userLink';
import { wsAverageLag } from 'common/socket';

export function userHtml(ctrl: RoundController, player: Player, position: Position): VNode {
  const d = ctrl.data,
    user = player.user,
    perf = (user?.perfs || {})[d.game.perf],
    rating = player.rating || perf?.rating,
    isPlayer = user?.id === d.player.user?.id && ctrl.isPlaying(),
    signal = user?.id === d.opponent.user?.id ? d.opponentSignal : isPlayer ? playerLag() : undefined;

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
          attrs: { 'data-pt-pos': 's', ...(ctrl.isPlaying() ? { target: '_blank' } : {}) },
          online: false,
          line: false,
        }),
        !!signal && signalBars(signal, isPlayer),
        !!rating && h('rating', rating + (player.provisional ? '?' : '')),
        !!rating && ratingDiff(player),
        player.engine &&
          h('span', {
            attrs: { 'data-icon': licon.CautionCircle, title: i18n.site.thisAccountViolatedTos },
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
      h('name', player.name || i18n.site.anonymous),
    ],
  );
}

const bars = (signal: number) => {
  const bars: VNode[] = [];
  for (let i = 1; i <= 4; i++) bars.push(h(i <= signal ? 'i' : 'i.off'));
  return bars;
};

const playerLag = () => {
  const ping = Math.round(wsAverageLag());
  return !ping ? 0 : ping < 150 ? 4 : ping < 300 ? 3 : ping < 500 ? 2 : 1;
};

const signalBars = (signal: number, isPlayer: boolean = false) => {
  return h(
    'signal.q' + signal,
    {
      hook: {
        insert(node) {
          const el = node.elm as HTMLSpanElement;
          if (isPlayer) {
            setInterval(() => {
              const signal = playerLag();
              el.className = 'q' + signal;
              node.children?.map(
                (n: VNode, i) => ((n.elm as HTMLElement).className = i < signal ? '' : 'off'),
              );
            }, 1000);
          }
        },
      },
    },
    bars(signal),
  );
};

export const userTxt = (player: Player): string =>
  player.user
    ? (player.user.title ? player.user.title + ' ' : '') + player.user.username
    : player.ai
      ? i18n.site.aiNameLevelAiLevel('Stockfish', player.ai)
      : i18n.site.anonymous;
