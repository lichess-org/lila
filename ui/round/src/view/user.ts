import { h } from 'snabbdom';
import * as licon from 'common/licon';
import { Player } from 'game';
import { Position } from '../interfaces';
import RoundController from '../ctrl';

export const aiName = (ctrl: RoundController, level: number) =>
  ctrl.trans('aiNameLevelAiLevel', 'Stockfish', level);

export function userHtml(ctrl: RoundController, player: Player, position: Position) {
  const d = ctrl.data,
    user = player.user,
    perf = (user?.perfs || {})[d.game.perf],
    rating = player.rating || perf?.rating,
    rd = player.ratingDiff,
    ratingDiff =
      rd === 0
        ? h('span', '±0')
        : rd && rd > 0
        ? h('good', '+' + rd)
        : rd && rd < 0
        ? h('bad', '−' + -rd)
        : undefined;

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
        h(
          `a.text${user.id == 'ghost' ? '' : '.ulpt'}`,
          {
            attrs: {
              'data-pt-pos': 's',
              href: '/@/' + user.username,
              ...(ctrl.isPlaying() ? { target: '_blank', rel: 'noopener' } : {}),
            },
          },
          user.title
            ? [
                h('span.utitle', user.title == 'BOT' ? { attrs: { 'data-bot': true } } : {}, user.title),
                ' ',
                user.username,
              ]
            : [user.username],
        ),
        rating ? h('rating', rating + (player.provisional ? '?' : '')) : null,
        rating ? ratingDiff : null,
        player.engine
          ? h('span', {
              attrs: {
                'data-icon': licon.CautionCircle,
                title: ctrl.noarg('thisAccountViolatedTos'),
              },
            })
          : null,
      ],
    );
  }
  const connecting = !player.onGame && ctrl.firstSeconds;
  return h(
    `div.ruser-${position}.ruser.user-link`,
    {
      class: {
        online: player.onGame,
        offline: !player.onGame,
        connecting,
      },
    },
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

export const userTxt = (ctrl: RoundController, player: Player) =>
  player.user
    ? (player.user.title ? player.user.title + ' ' : '') + player.user.username
    : player.ai
    ? aiName(ctrl, player.ai)
    : ctrl.noarg('anonymous');
