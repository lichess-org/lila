var m = require('mithril');
var axis = require('./axis');
var filters = require('./filters');
var chart = require('./chart');
var table = require('./table');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading',
    // config: function(el, isUpdate) {
    //   if (isUpdate) return;
    //   $(el).find('.ms-parent').each(function() {
    //     $(this).hover(function() {
    //       $(this).prev().multipleSelect('open');
    //     }, function() {
    //       $(this).prev().multipleSelect('close');
    //     });
    //   });
    // }
  }, [
    m('div.left', [
      filters(ctrl),
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
    m('div.top', [
      axis(ctrl),
      m('h2', [
        m('a', {
          href: '/@/' + ctrl.username,
        }, ctrl.username),
        '\'s chess insights'
      ])
    ]),
    chart(ctrl),
    table.vert(ctrl)
  ]);
};
