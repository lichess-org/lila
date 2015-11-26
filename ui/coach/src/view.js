var m = require('mithril');
var chart = require('./chart');
var form = require('./form');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.answer ? '' : 'loading',
  }, [
    form.filters(ctrl),
    form.axis(ctrl),
    chart(ctrl)
  ]);
};
