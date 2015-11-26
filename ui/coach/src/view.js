var m = require('mithril');
var form = require('./form');
var chart = require('./chart');
var table = require('./table');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading',
  }, [
    form.filters(ctrl),
    form.axis(ctrl),
    chart(ctrl),
    // table.horiz(ctrl),
    table.vert(ctrl)
  ]);
};
