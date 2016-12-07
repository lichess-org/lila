var m = require('mithril');

// useful in translation arguments
function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

module.exports = function(ctrl) {

  var puzzle = ctrl.getData().puzzle;
  var game = ctrl.getData().game;

  return [
    m('div.game_infos.puzzle[data-icon="-"]', [
      m('div.header', [
        m('a.title', {
          href: '/training/' + puzzle.id
        }, ctrl.trans('puzzleId', puzzle.id)),
        m('p', m.trust(ctrl.trans('ratingX',
          strong(ctrl.vm.mode === 'view' ? puzzle.rating : '?')))),
        m('p', m.trust(ctrl.trans('playedXTimes',
          strong(lichess.numberFormat(puzzle.attempts)))))
      ])
    ]),
    m('div.game_infos.game[data-icon="-"]', {
      'data-icon': game.perf.icon
    }, [
      m('div.header', [
        'From game ',
        ctrl.vm.mode === 'view' ? m('a.title', {
          href: '/' + game.id
        }, '#' + game.id) : '#' + game.id.slice(0, 5) + '...',
        m('p', [
          game.clock, ' • ',
          game.perf.name, ' • ',
          ctrl.trans.noarg(game.rated ? 'rated' : 'casual')
        ])
      ])
    ]),
    m('div.players', game.players.map(function(p) {
      return m('div.player.color-icon.is.text.' + p.color,
        p.userId ? m('a.user_link.ulpt', {
          href: '/@/' + p.userId
        }, p.name) : p.name
      );
    }))
  ];
};
