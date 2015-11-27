var m = require('mithril');
var form = require('./form');
var chart = require('./chart');
var table = require('./table');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading',
  }, [
    m('div.left', [
      form.filters(ctrl),
      m('div.refresh', {
        config: function(e, isUpdate) {
          if (isUpdate) return;
          var $ref = $('.insight-stale');
          if ($ref.length) {
            $(e).append($ref.show());
            lichess.refreshInsightForm();
          }
        }
      })
    ]),
    form.axis(ctrl),
    chart(ctrl),
    // table.horiz(ctrl),
    table.vert(ctrl)
  ]);
};
