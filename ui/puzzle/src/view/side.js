var m = require('mithril');

// useful in translation arguments
function strong(txt) {
  return '<strong>' + txt + '</strong>';
}

function puzzleBox(ctrl) {
  var data = ctrl.getData();
  return m('div.side_box.metas', [
    puzzleInfos(ctrl, data.puzzle),
    gameInfos(ctrl, data.game, data.puzzle)
  ])
}

function puzzleInfos(ctrl, puzzle) {

  return m('div.game_infos.puzzle[data-icon="-"]', [
    m('div.header', [
      m('a.title', {
        href: '/training/' + puzzle.id
      }, ctrl.trans('puzzleId', puzzle.id)),
      m('p', m.trust(ctrl.trans('ratingX',
        strong(ctrl.vm.mode !== 'play' ? puzzle.rating : '?')))),
      m('p', m.trust(ctrl.trans('playedXTimes',
        strong(lichess.numberFormat(puzzle.attempts)))))
    ])
  ]);
}

function gameInfos(ctrl, game, puzzle) {
  return [
    m('div.game_infos.game[data-icon="-"]', {
      'data-icon': game.perf.icon
    }, [
      m('div.header', [
        'From game ',
        ctrl.vm.mode !== 'play' ? m('a.title', {
          href: '/' + game.id + '/' + puzzle.color + '#' + puzzle.initialPly
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
}

function userBox(ctrl) {
  var data = ctrl.getData();
  if (!data.user) return;
  var ratingHtml = data.user.rating;
  if (ctrl.vm.round) {
    var diff = ctrl.vm.round.ratingDiff,
      klass = '';
    if (diff >= 0) {
      diff = '+' + diff;
      if (diff > 0) klass = 'up';
    } else if (diff === 0) diff = '+0';
    else klass = 'down';
    ratingHtml += ' <span class="rp ' + klass + '">' + diff + '</span>';
  }
  return m('div.side_box.rating', [
    m('h2', m.trust(ctrl.trans('yourPuzzleRatingX', strong(ratingHtml)))),
    m('div', ratingChart(ctrl))
  ]);
}

function ratingChart(ctrl) {
  return m('div.rating_chart', {
    config: function(el, isUpdate, ctx) {
      var hash = ctrl.recentHash();
      if (hash == ctx.hash) return;
      ctx.hash = hash;
      var dark = document.body.classList.contains('dark');
      var points = ctrl.getData().user.recent.map(function(r) {
        return r[2] + r[1];
      });
      jQuery(el).sparkline(points, {
        type: 'line',
        width: '224px',
        height: '80px',
        lineColor: dark ? '#4444ff' : '#0000ff',
        fillColor: dark ? '#222255' : '#ccccff'
      });
    }
  });
}

module.exports = function(ctrl) {

  return [
    puzzleBox(ctrl),
    userBox(ctrl)
  ];
};
