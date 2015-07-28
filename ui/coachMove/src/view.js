var m = require('mithril');

var inspect = require('./inspect');
var table = require('./table');
var Slider = require('coach').slider;
var shared = require('coach').shared;

module.exports = function(ctrl) {
  if (!ctrl.nbPeriods) return m('div.content_box_top', [
    m('h1', [
      shared.userLink(ctrl.user.name),
      ' moves: No data available'
    ]),
  ]);
  return m('div', {
    config: function() {
      $('body').trigger('lichess.content_loaded');
    }
  }, [
    m('div.content_box_top', {
      class: 'content_box_top' + (ctrl.vm.loading ? ' loading' : '')
    }, [
      ctrl.nbPeriods > 1 ? m.component(Slider, {
        max: ctrl.nbPeriods,
        range: ctrl.vm.range,
        dates: ctrl.data ? [ctrl.data.from, ctrl.data.to] : null,
        onChange: ctrl.selectPeriodRange
      }) : null,
      m('h1', [
        shared.userLink(ctrl.user.name),
        ' moves',
        ctrl.data ? m('div.over', [
          ' over ',
          ctrl.data.perfs[0].results.base.nbGames,
          ' games'
        ]) : null
      ]),
    ]),
    ctrl.vm.preloading ? m('div.loader') : (!ctrl.data ? m('div.top.nodata', m('p', 'Empty period range!')) : [
      inspect(ctrl),
      table(ctrl)
    ])
  ]);
};
