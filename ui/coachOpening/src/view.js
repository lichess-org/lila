var m = require('mithril');

var piechart = require('./piechart');
var table = require('./table');
var inspect = require('./inspect');
var Slider = require('./slider');

module.exports = function(ctrl) {
  if (!ctrl.nbPeriods) return m('div.content_box_top', [
    m('h1', [
      ctrl.user.name,
      ' openings as ',
      ctrl.color,
      ': ',
      'No data available'
    ]),
  ]);
  return [
    m('div.content_box_top', {
      class: 'content_box_top' + (ctrl.vm.loading ? ' loading' : '')
    }, [
      ctrl.nbPeriods > 1 ? m.component(Slider, {
        max: ctrl.nbPeriods,
        range: ctrl.vm.range,
        onChange: ctrl.selectPeriodRange
      }) : null,
      m('h1', [
        ctrl.user.name,
        ' openings as ',
        ctrl.color,
        ctrl.data ? m('div.over', [
          ' over ',
          ctrl.data.colorResults.nbGames,
          ' games.'
        ]) : null
      ]),
    ]),
    ctrl.vm.preloading ? m('div.loader') : [
      ctrl.vm.inspecting ? inspect(ctrl, ctrl.vm.inspecting) : m('div.top.chart', {
        config: function(el, isUpdate, ctx) {
          if (ctx.chart) piechart.update(ctx.chart, ctrl);
          else ctx.chart = piechart.create(el, ctrl);
        }
      }),
      table(ctrl)
    ]
  ];
};
