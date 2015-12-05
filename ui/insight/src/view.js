var m = require('mithril');
var axis = require('./axis');
var filters = require('./filters');
var presets = require('./presets');
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
      m('div.panel-tabs', [
        m('a[data-panel=preset]', {
          class: 'tab' + (ctrl.vm.panel === 'preset' ? ' active' : ''),
          onclick: function() {
            ctrl.vm.panel = 'preset';
          }
        }, 'Presets'),
        m('a[data-panel=filter]', {
          class: 'tab' + (ctrl.vm.panel === 'filter' ? ' active' : ''),
          onclick: function() {
            ctrl.vm.panel = 'filter';
          }
        }, 'Filters'), !!Object.keys(ctrl.vm.filters).length ? m('a.clear.hint--top', {
          'data-hint': 'Clear all filters',
          onclick: ctrl.clearFilters
        }, m('span', {
          'data-icon': 'L',
        }, 'CLEAR')) : null,
      ]),
      ctrl.vm.panel === 'filter' ? filters(ctrl) : null,
      ctrl.vm.panel === 'preset' ? presets(ctrl) : null,
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
