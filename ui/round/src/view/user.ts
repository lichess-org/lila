import { game } from 'game';

import { h } from 'snabbdom'

function ratingDiff(player) {
  if (player.ratingDiff === 0) return h('span.rp.null', '±0');
  if (player.ratingDiff > 0) return h('span.rp.up', '+' + player.ratingDiff);
  if (player.ratingDiff < 0) return h('span.rp.down', player.ratingDiff);
  return;
}

export function aiName(ctrl, player) {
  return ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai);
}

export function userHtml(ctrl, player) {
  var d = ctrl.data;
  var user = player.user;
  var perf = user ? user.perfs[d.game.perf] : null;
  var rating = player.rating ? player.rating : (perf && perf.rating);
  if (user) {
    var connecting = !player.onGame && ctrl.vm.firstSeconds && user.online;
    return h('div.username.user_link.' + player.color, {
      class: {
        online: player.onGame,
        offline: !player.onGame,
        long: user.username.length > 16,
        connecting: connecting
      }
    }, [
      h('i.line' + (user.patron ? '.patron' : ''), {
        attrs: {
          title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
        }
      }),
      h('a.text.ulpt', {
        attrs: {
          'data-pt-pos': 's',
          href: '/@/' + user.username,
          target: game.isPlayerPlaying(d) ? '_blank' : '_self'
        }
      }, user.title ? [h('span.title', user.title), ' ', user.username] : user.username),
      rating ? h('rating', rating + (player.provisional ? '?' : '')) : null,
      ratingDiff(player),
      player.engine ? h('span', {
        attrs: {
          'data-icon': 'j',
          title: ctrl.trans.noarg('thisPlayerUsesChessComputerAssistance')
        }
      }) : null
    ]);
  }
  var connecting = !player.onGame && ctrl.vm.firstSeconds;
  return h('div.username.user_link', {
    class: {
      online: player.onGame,
      offline: !player.onGame,
      connecting: connecting
    }
  }, [
    h('i.line', {
      attrs: {
        title: connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
      }
    }),
    h('name', player.name || 'Anonymous')
  ]);
};
export function userTxt(ctrl, player) {
  if (player.user) {
    var perf = player.user.perfs[ctrl.data.game.perf];
    var name = (player.user.title ? player.user.title + ' ' : '') + player.user.username;
    var rating = player.rating ? player.rating : (perf ? perf.rating : null);
    rating = rating ? ' (' + rating + (player.provisional ? '?' : '') + ')' : '';
    return name + rating;
  } else if (player.ai) return aiName(ctrl, player)
  else return 'Anonymous';
};
