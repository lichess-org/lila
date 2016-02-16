var m = require('mithril');
var partial = require('chessground').util.partial;
var util = require('./util');
var status = require('game').status;

function result(win, stat) {
  switch (win) {
    case true:
      return '1';
    case false:
      return '0';
    default:
      return stat >= status.ids.mate ? '½' : '*';
  }
}

function playerTitle(player) {
  return m('h2', [
    m('span.rank', player.rank + '. '),
    util.player(player)
  ]);
}

module.exports = function(ctrl) {
  var data = ctrl.vm.playerInfo.data;
  if (!data || data.player.id !== ctrl.vm.playerInfo.id) return m('div.player', [
    playerTitle(ctrl.vm.playerInfo.player),
    m('div.stats', m.trust(lichess.spinnerHtml))
  ]);
  var nb = data.player.nb;
  var pairingsLen = data.pairings.length
  var avgOp = pairingsLen ? Math.round(data.pairings.reduce(function(a, b) {
    return a + b.op.rating;
  }, 0) / pairingsLen) : null;
  return m('div.box.player', {
    config: function(el, isUpdate) {
      if (!isUpdate) $('body').trigger('lichess.content_loaded');
    }
  }, [
    m('close[data-icon=L]', {
      onclick: partial(ctrl.showPlayerInfo, data.player)
    }),
    playerTitle(data.player),
    m('div.stats', m('table', [
      m('tr', [m('th', 'Games played'), m('td', nb.game)]),
      nb.game ? [
        m('tr', [m('th', 'Win rate'), m('td', util.ratio2percent(nb.win / nb.game))]),
        m('tr', [m('th', 'Berserk rate'), m('td', util.ratio2percent(nb.berserk / nb.game))]),
        m('tr', [m('th', 'Average opponent'), m('td', avgOp)]),
        data.player.performance ? m('tr', [m('th', 'Performance'), m('td', data.player.performance)]) : null
      ] : null
    ])),
    m('div.scroll-shadow-soft', m('table.pairings', {
      onclick: function(e) {
        var href = e.target.parentNode.getAttribute('data-href');
        if (href) window.open(href, '_blank');
      }
    }, data.pairings.map(function(p, i) {
      var res = result(p.win, p.status);
      return m('tr', {
        key: p.id,
        'data-href': '/' + p.id + '/' + p.color,
        class: 'glpt' + (res === '1' ? ' win' : (res === '0' ? ' loss' : '')),
        config: function(el, isUpdate, ctx) {
          if (!isUpdate) ctx.onunload = function() {
            $.powerTip.destroy(el);
          };
        }
      }, [
        m('th', Math.max(nb.game, pairingsLen) - i),
        m('td', (p.op.title ? p.op.title + ' ' : '') + p.op.name),
        m('td', p.op.rating),
        m('td', {
          'class': 'is color-icon ' + p.color
        }),
        m('td', res)
      ]);
    })))
  ]);
};
