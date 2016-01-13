var m = require('mithril');
var util = require('chessground').util;

function initialize(ctrl, el) {
  var $div = $(el);
  var $ratingRange = $div.find('.rating_range');
  var save = $.fp.debounce(function() {
    var $form = $div.find('form');
    $.ajax({
      url: $form.attr('action'),
      data: $form.serialize(),
      type: 'POST',
      success: function(filter) {
        ctrl.setFilter(filter);
      }
    });
  }, 200);
  function changeRatingRange(values) {
    $ratingRange.find('input').val(values[0] + "-" + values[1]);
    $ratingRange.siblings('.range').text(values[0] + " - " + values[1]);
    save();
  }
  $div.find('input').change(save);
  $div.find('button.reset').click(function() {
    $div.find('label input').prop('checked', true).trigger('change');
    $div.find('.rating_range').each(function() {
      var s = $(this);
      var values = [s.slider('option', 'min'), s.slider('option', 'max')];
      s.slider('values', values);
      changeRatingRange(values);
    });
  });
  $div.find('button').click(function() {
    ctrl.toggleFilter();
    m.redraw();
    return false;
  });
  $ratingRange.each(function() {
    var $this = $(this);
    var $input = $this.find("input");
    var $span = $this.siblings(".range");
    var min = $input.data("min");
    var max = $input.data("max");
    var values = $input.val() ? $input.val().split("-") : [min, max];
    $span.text(values.join(' - '));

    $this.slider({
      range: true,
      min: min,
      max: max,
      values: values,
      step: 50,
      slide: function(e, ui) {
        changeRatingRange(ui.values);
      }
    });
  });
}

module.exports = {
  toggle: function(ctrl, nbFiltered) {
    return m('span', {
      class: 'filter_toggle' + (ctrl.vm.filterOpen ? ' active' : ''),
      onclick: util.partial(ctrl.toggleFilter)
    }, [
      ctrl.vm.filterOpen ? m('span[data-icon=L]') : m('span', {
        class: 'hint--bottom-left',
        'data-hint': ctrl.trans('filterGames'),
      }, m('span[data-icon=%]')),
      nbFiltered > 0 ? m('span.number', nbFiltered) : null
    ]);
  },
  render: function(ctrl) {
    return m('div.hook_filter', {
      config: function(el, isUpdate, ctx) {
        if (ctx.loaded) return;
        $.ajax({
          url: '/setup/filter',
          success: function(html) {
            el.innerHTML = html;
            ctx.loaded = true;
            initialize(ctrl, el);
          }
        });
      }
    });
  }
};
