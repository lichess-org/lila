import { h } from 'snabbdom';
import { bind } from '../util';
import LobbyController from '../../ctrl';

function initialize(ctrl: LobbyController, el) {
  const $div = $(el),
    $ratingRange = $div.find('.rating-range');

  const save = window.lichess.debounce(function() {
    const $form = $div.find('form');
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
    $ratingRange.siblings('.range').text(values[0] + "–" + values[1]);
    save();
  }
  $div.find('input').change(save);
  $div.find('button.reset').click(function() {
    $div.find('label input').prop('checked', true).trigger('change');
    $div.find('.rating-range').each(function(this: HTMLElement) {
      const s = $(this),
        values = [s.slider('option', 'min'), s.slider('option', 'max')];
      s.slider('values', values);
      changeRatingRange(values);
    });
    return false;
  });
  $div.find('button.apply').click(function() {
    ctrl.toggleFilter();
    ctrl.redraw();
    return false;
  });
  $ratingRange.each(function(this: HTMLElement) {
    var $this = $(this);
    window.lichess.slider().done(function() {
      var $input = $this.find("input");
      var $span = $this.siblings(".range");
      var min = $input.data("min");
      var max = $input.data("max");
      var values = $input.val() ? $input.val().split("-") : [min, max];
      $span.text(values.join('–'));
      $this.slider({
        range: true,
        min: min,
        max: max,
        values: values,
        step: 50,
        slide(_, ui) {
          changeRatingRange(ui.values);
        }
      });
    });
  });
}

export function toggle(ctrl: LobbyController, nbFiltered: number) {
  return h('i.toggle.toggle-filter', {
    class: { active: ctrl.filterOpen },
    hook: bind('mousedown', ctrl.toggleFilter, ctrl.redraw),
    attrs: {
      'data-icon': ctrl.filterOpen ? 'L' : '%',
      title: ctrl.trans.noarg('filterGames')
    }
  }, nbFiltered > 0 ? '' + nbFiltered : '');
}

export interface FilterNode extends HTMLElement {
  filterLoaded?: boolean;
}

export function render(ctrl: LobbyController) {
  return h('div.hook__filters', {
    hook: {
      insert(vnode) {
        const el = vnode.elm as FilterNode;
        if (el.filterLoaded) return;
        $.ajax({
          url: '/setup/filter',
          success(html) {
            el.innerHTML = html;
            el.filterLoaded = true;
            initialize(ctrl, el);
          }
        });
      }
    }
  });
}
