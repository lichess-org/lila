var m = require('mithril');
var axis = require('./axis');
var filters = require('./filters');
var chart = require('./chart');
var table = require('./table');
var help = require('./help');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading'
  }, [
    m('div.left', [
      m('div.info.box', [
        m('div.top', [
          m('a.username.user_link.ulpt', {
            href: '/@/' + ctrl.user.name
          }, ctrl.user.name)
        ]),
        m('div.content', [
          m('p.nbGames', 'Insights over ' + ctrl.user.nbGames + ' rated games.'),
          m('div.refresh', {
            config: function(e, isUpdate) {
              if (isUpdate) return;
              var $ref = $('.insight-stale');
              if ($ref.length) {
                $(e).html($ref.show());
                lichess.refreshInsightForm();
              }
            }
          })
        ])
      ]),
      filters(ctrl),
      help(ctrl)
    ]),
    m('header', [
      axis(ctrl),
      m('h2', 'Chess Insights')
    ]),
    chart(ctrl),
    table.vert(ctrl)
  ]);
};
