import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Player } from 'game';
import RoundController from '../ctrl';

function ratingDiff(player: Player): VNode | undefined {
  if (player.ratingDiff === 0) return h('span.rp.null', '±0');
  if (player.ratingDiff && player.ratingDiff > 0) return h('span.rp.up', '+' + player.ratingDiff);
  if (player.ratingDiff && player.ratingDiff < 0) return h('span.rp.down', '−' + (-player.ratingDiff));
  return;
}

export function aiName(ctrl: RoundController, level: number) {
  return ctrl.trans('aiNameLevelAiLevel', 'Stockfish', level);
}

export function userHtml(ctrl: RoundController, player: Player) {
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : (perf && perf.rating);
  if (user) {
    const connecting = !player.onGame && ctrl.firstSeconds && user.online;
    const link = h('a.text.ulpt', {
      attrs: {
        'data-pt-pos': 's',
        href: '/@/' + user.username,
        target: ctrl.isPlaying() ? '_blank' : '_self'
      }
    }, user.title ? [
      h(
        'span.title',
        user.title == 'BOT' ? { attrs: {'data-bot': true } } : {},
        user.title
      ), ' ', user.username
    ] : [user.username]);
    const ratingTag = rating ? h('rating', rating + (player.provisional ? '?' : '')) : null;
    return ctrl.blind ? h('span', [link, ratingTag]) : h('div.username.user_link.' + player.color, {
      class: {
        online: player.onGame,
        offline: !player.onGame,
        long: user.username.length > 16,
        connecting
      }
    }, [
      h('i.line' + (user.patron ? '.patron' : ''), {
        attrs: {
          title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
        }
      }),
      link,
      ratingTag,
      ratingDiff(player),
      player.engine ? h('span', {
        attrs: {
          'data-icon': 'j',
          title: ctrl.trans.noarg('thisPlayerUsesChessComputerAssistance')
        }
      }) : null
    ]);
  }
  const connecting = !player.onGame && ctrl.firstSeconds;
  const name = h('name', player.name || 'Anonymous');
  return ctrl.blind ? name : h('div.username.user_link', {
    class: {
      online: player.onGame,
      offline: !player.onGame,
      connecting
    }
  }, [
    h('i.line', {
      attrs: {
        title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
      }
    }),
    name
  ]);
}

export function userTxt(ctrl: RoundController, player: Player) {
  if (player.user) {
    return (player.user.title ? player.user.title + ' ' : '') + player.user.username;
  } else if (player.ai) return aiName(ctrl, player.ai)
  else return 'Anonymous';
}
