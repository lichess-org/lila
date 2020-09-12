import { h } from 'snabbdom';
import * as xhr from 'common/xhr';
import { bind } from '../util';
import LobbyController from '../../ctrl';

function initialize(ctrl: LobbyController, el: HTMLElement) {

  const f = ctrl.filter.data?.form,
    $div = $(el),
    $ratingRange = $div.find('.rating-range');

  if (f) Object.keys(f).forEach(k => {
    const input = $div.find(`input[name="${k}"]`)[0] as HTMLInputElement;
    if (input.type == 'checkbox') input.checked = true;
    else input.value = f[k];
  });
  else $div.find('input').prop('checked', true)

  const save = () => ctrl.filter.save($div.find('form')[0] as HTMLFormElement);

  function changeRatingRange(values) {
    $ratingRange.find('input').val(values[0] + "-" + values[1]);
    $ratingRange.siblings('.range').text(values[0] + "–" + values[1]);
    save();
  }
  $div.find('input').change(save);
  $div.find('button.reset').click(function() {
    ctrl.filter.set(null);
    ctrl.filter.open = false;
    ctrl.redraw();
  });
  $div.find('button.apply').click(function() {
    ctrl.filter.open = false;
    ctrl.redraw();
  });
  $ratingRange.each(function(this: HTMLElement) {
    var $this = $(this);
    window.lichess.slider().then(() => {
      const $input = $this.find("input"),
        $span = $this.siblings(".range"),
        min = $input.data("min"),
        max = $input.data("max"),
        values = $input.val() ? $input.val().split("-") : [min, max];
      $span.text(values.join('–'));
      $this.slider({
        range: true,
        min,
        max,
        values,
        step: 50,
        slide(_, ui) {
          changeRatingRange(ui.values);
        }
      });
    });
  });
}

export function toggle(ctrl: LobbyController, nbFiltered: number) {
  const filter = ctrl.filter;
  return h('i.toggle.toggle-filter', {
    class: { gamesFiltered: nbFiltered > 0, active: filter.open },
    hook: bind('mousedown', filter.toggle, ctrl.redraw),
    attrs: {
      'data-icon': filter.open ? 'L' : '%',
      title: ctrl.trans.noarg('filterGames')
    }
  });
}

export interface FilterNode extends HTMLElement {
  filterLoaded?: boolean;
}

export const render = (ctrl: LobbyController) =>
  h('div.hook__filters', {
    hook: {
      insert(vnode) {
        const el = vnode.elm as FilterNode;
        if (el.filterLoaded) return;
        xhr.text('/setup/filter')
          .then(html => {
            el.innerHTML = html;
            el.filterLoaded = true;
            initialize(ctrl, el);
          });
      }
    }
  });
