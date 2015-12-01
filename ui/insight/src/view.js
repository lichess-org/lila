var m = require('mithril');
var axis = require('./axis');
var filters = require('./filters');
var chart = require('./chart');
var table = require('./table');
var help = require('./help');
var info = require('./info');
var boards = require('./boards');

module.exports = function(ctrl) {
  return m('div', {
    class: ctrl.vm.loading ? 'loading' : 'ready',
    config: function(el, isUpdate) {
      if (isUpdate) return;
      setTimeout(function() {
        lichess.userPowertip($('.insight-ulpt'), 'e');
      }, 600);
    }
  }, [
    m('div.left-side', [
      info(ctrl),
      filters(ctrl),
      help(ctrl)
    ]),
    m('header', [
      axis(ctrl),
      m('h2', {
        class: 'text',
        'data-icon': '7'
      }, 'Chess Insights [BETA]')
    ]),
    chart(ctrl),
    table.vert(ctrl),
    boards(ctrl)
  ]);
};
