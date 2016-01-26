var m = require('mithril');
var partial = require('chessground').util.partial;

var boardContent = m('div.cg-board-wrap', m('div.cg-board'));

function miniBoard(game) {
  return m('a', {
    key: game.id,
    href: '/' + game.id + (game.color === 'white' ? '' : '/black'),
    class: 'mini_board live_' + game.id + ' parse_fen is2d',
    'data-color': game.color,
    'data-fen': game.fen,
    'data-lastmove': game.lastMove,
    config: function(el, isUpdate) {
      if (!isUpdate) lichess.parseFen($(el));
    }
  }, boardContent);
}

module.exports = {
  title: function(ctrl) {
    if (ctrl.data.schedule && ctrl.data.schedule.freq.indexOf('marathon') !== -1)
      return m('h1.marathon_title', [
        m('span.fire_trophy.marathonWinner', m('span[data-icon=\\]')),
        ctrl.data.fullName
      ]);
    return m('h1', {
      class: 'text',
      'data-icon': ctrl.data.isFinished ? '' : 'g'
    }, [
      ctrl.data.greatPlayer ? [
        m('a', {
          href: ctrl.data.greatPlayer.url,
          target: '_blank'
        }, ctrl.data.greatPlayer.name),
        ' tournament'
      ] : ctrl.data.fullName,
      ctrl.data.private ? [
        ' ',
        m('span.text[data-icon=a]', ctrl.trans('isPrivate'))
      ] : null
    ]);
  },
  currentPlayer: function(ctrl, pag) {
    if (!ctrl.userId || !pag.currentPageResults) return null;
    return pag.currentPageResults.filter(function(p) {
      return p.name.toLowerCase() === ctrl.userId;
    })[0] || null;
  },
  player: function(p, tag) {
    var ratingDiff, tag = tag || 'a';
    if (p.ratingDiff > 0) ratingDiff = m('span.positive[data-icon=N]', p.ratingDiff);
    else if (p.ratingDiff < 0) ratingDiff = m('span.negative[data-icon=M]', -p.ratingDiff);
    var rating = p.rating + p.ratingDiff + (p.provisional ? '?' : '');
    var fullName = (p.title ? p.title + ' ' : '') + p.name;
    var attrs = {
      class: 'ulpt user_link' + (fullName.length > 15 ? ' long' : ''),
      config: function(el, isUpdate, ctx) {
        if (!isUpdate) ctx.onunload = function() {
          $.powerTip.destroy(el);
        };
      }
    };
    attrs[tag === 'a' ? 'href' : 'data-href'] = '/@/' + p.name;
    return {
      tag: tag,
      attrs: attrs,
      children: [
        fullName,
        m('span.progress', [rating, ratingDiff])
      ]
    };
  },
  miniBoard: miniBoard,
  clock: function(time) {
    return function(el, isUpdate) {
      if (!isUpdate) $(el).clock({
        time: time
      });
    };
  },
  ratio2percent: function(r) {
    return Math.round(100 * r) + '%';
  }
};
