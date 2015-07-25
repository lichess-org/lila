var m = require('mithril');

var cubeFacets = ['a', 'b', 'c', 'd'].map(function(x) {
  return m('div.' + x, m('div'));
});

module.exports = {
  controller: function(args) {
    this.max = args.max;
    this.range = args.range;
  },
  view: function(ctrl, args) {
    var ratio = function(x) {
      return x * 100 / ctrl.max;
    };
    return m('div.cube', [
      cubeFacets,
      m('div.slider', {
        config: function(el, isUpdate) {
          if (isUpdate) return;
          $(el).slider({
            range: true,
            min: 0,
            max: ctrl.max,
            values: ctrl.range,
            slide: function(event, ui) {
              $(el).parent().find('.a div, .b div, .c div, .d div').css({
                width: ratio(ui.values[1] - ui.values[0]) + "%",
                marginLeft: ratio(ui.values[0]) + '%'
              });
              args.onChange(ui.values[0], ui.values[1]);
            }
          }).find('.ui-slider-handle').text('<>');
        }
      })
    ]);
  }
};
