var m = require('mithril');
var vn = require('mithril/render/vnode');
var game = require('game').game;
var raf = require('chessground').util.requestAnimationFrame;

function rpSpan(klass, text) {
  return vn('span', undefined, {
    class: 'rp ' + klass
  }, undefined, text);
}

function ratingDiff(player) {
  if (typeof player.ratingDiff === 'undefined') return null;
  if (player.ratingDiff === 0) return rpSpan('null', '±0');
  if (player.ratingDiff > 0) return rpSpan('up', '+' + player.ratingDiff);
  if (player.ratingDiff < 0) return rpSpan('down', player.ratingDiff);
}

function relayUser(player) {
  return m('span', {
    class: 'text',
    'data-icon': '8'
  }, [
    (player.title ? player.title + ' ' : '') + player.name,
    player.rating ? ' (' + player.rating + ')' : ''
  ]);
}

var aiNameCache = {};

function aiName(ctrl, player) {
  if (!aiNameCache[player.ai])
    aiNameCache[player.ai] = ctrl.trans('aiNameLevelAiLevel', 'Stockfish', player.ai);
  return aiNameCache[player.ai];
}

module.exports = {
  userHtml: function(ctrl, player) {
    var d = ctrl.data;
    var user = player.user;
    if (d.relay) return relayUser(d.relay[player.color]);
    var perf = user ? user.perfs[d.game.perf] : null;
    var rating = player.rating ? player.rating : (perf ? perf.rating : null);
    if (user) {
      var fullName = (user.title ? user.title + ' ' : '') + user.username;
      var connecting = !player.onGame && ctrl.vm.firstSeconds && user.online;
      var isMe = ctrl.userId === user.id;
      return vn('div', 'user-' + player.color, {
        class: 'username user_link ' + player.color + ' ' +
          (player.onGame ? 'online' : 'offline') +
          (fullName.length > 20 ? ' long' : '') +
          (connecting ? ' connecting' : '')
      }, [
        vn('i', undefined, {
          class: 'line' + (user.patron ? ' patron' : ''),
          'title': connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
        }),
        vn('a', undefined, {
          class: 'text ulpt',
          'data-pt-pos': 's',
          href: '/@/' + user.username,
          target: game.isPlayerPlaying(d) ? '_blank' : '_self',
        }, undefined, fullName),
        rating ? vn('rating', undefined, undefined, undefined, rating + (player.provisional ? '?' : '')) : null,
        ratingDiff(player),
        player.engine ? m('span[data-icon=j]', {
          title: ctrl.trans.noarg('thisPlayerUsesChessComputerAssistance')
        }) : null
      ]);
    }
    var connecting = !player.onGame && ctrl.vm.firstSeconds;
    return vn('div', 'user-' + player.color, {
      class: 'username user_link ' +
        (player.onGame ? 'online' : 'offline') +
        (connecting ? ' connecting' : ''),
    }, [
      vn('i', undefined, {
        class: 'line',
        'title': connecting ? 'Connecting to the game' : (player.onGame ? 'Joined the game' : 'Left the game')
      }),
      vn('name', undefined, undefined, undefined, player.name || 'Anonymous')
    ]);
  },
  userTxt: function(ctrl, player) {
    if (player.user) {
      var perf = player.user.perfs[ctrl.data.game.perf];
      var name = (player.user.title ? player.user.title + ' ' : '') + player.user.username;
      var rating = player.rating ? player.rating : (perf ? perf.rating : null);
      rating = rating ? ' (' + rating + (player.provisional ? '?' : '') + ')' : '';
      return name + rating;
    } else if (player.ai) return aiName(ctrl, player)
    else return 'Anonymous';
  },
  aiHtml: function(ctrl, player) {
    return vn('div', 'user-' + player.color, {
      class: 'username user_link online',
    }, [
      vn('i', undefined, {
        class: 'line'
      }),
      vn('name', undefined, undefined, undefined, aiName(ctrl, player))
    ])
  }
};
