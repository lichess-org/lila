import { h } from 'snabbdom';

import * as licon from 'lib/licon';
import { bind } from 'lib/view';
import * as xhr from 'lib/xhr';

import type LobbyController from '@/ctrl';

function initialize(ctrl: LobbyController, el: FilterNode) {
  const f = ctrl.filter.data?.form,
    $div = $(el),
    $ratingRange = $div.find('.rating-range'),
    $rangeInput = $ratingRange.find('input[name="ratingRange"]'),
    $minInput = $ratingRange.find('.rating-range__min'),
    $maxInput = $ratingRange.find('.rating-range__max');

  if (f)
    Object.keys(f).forEach(k => {
      const input = $div.find(`input[name="${k}"]`)[0] as HTMLInputElement;
      if (!input) return;
      if (input.type === 'checkbox') input.checked = true;
      else input.value = f[k];
    });
  else $div.find('input').prop('checked', true);

  const save = () => ctrl.filter.save($div.find('form')[0] as HTMLFormElement);

  $div.find('input').on('change', save);
  $div
    .find('form')
    .on('reset', (e: Event) => {
      e.preventDefault();
      ctrl.filter.save(null);
      ctrl.filter.uiCacheBuster++;
      ctrl.redraw();
    })
    .on('submit', (e: SubmitEvent) => {
      e.preventDefault();
      ctrl.filter.open = false;
      ctrl.redraw();
    });

  function changeRatingRange(e?: Event) {
    const minVal = $minInput.val() as string;
    const maxVal = $maxInput.val() as string;

    $rangeInput.val(minVal + '-' + maxVal);
    $minInput.attr({ max: maxVal, 'aria-label': i18n.site.maxRatingX(minVal) });
    $maxInput.attr({ min: minVal, 'aria-label': i18n.site.minRatingX(maxVal) });

    const $range = $ratingRange.siblings('.range').empty();
    $('<span>').text(minVal).appendTo($range);
    $('<span>').text('–').appendTo($range);
    $('<span>').text(maxVal).appendTo($range);

    if (e) save();
  }

  const rangeValues = $rangeInput.val() ? ($rangeInput.val() as string).split('-') : [];

  const minValue = rangeValues[0] || $minInput.attr('min')!;
  $minInput.attr({ step: '50', value: minValue }).on('input', changeRatingRange);

  const maxValue = rangeValues[1] || $minInput.attr('max')!;
  $maxInput.attr({ step: '50', value: maxValue }).on('input', changeRatingRange);

  changeRatingRange();
}

export function toggle({ filter, redraw }: LobbyController, nbFiltered: number) {
  return h('button.toggle.toggle-filter', {
    class: { gamesFiltered: nbFiltered > 0, active: filter.open },
    hook: bind('click', filter.toggle, redraw),
    attrs: {
      'data-icon': filter.open ? licon.X : licon.Gear,
      title: filter.open ? i18n.site.close : i18n.site.filterGames,
    },
  });
}

export interface FilterNode extends HTMLElement {
  filterLoaded?: boolean;
}

export const render = (ctrl: LobbyController) =>
  h('div.hook__filters.cache-buster-' + ctrl.filter.uiCacheBuster, {
    hook: {
      insert(vnode) {
        const el = vnode.elm as FilterNode;
        if (el.filterLoaded) return;
        xhr.text('/setup/filter').then(html => {
          el.innerHTML = html;
          el.filterLoaded = true;
          initialize(ctrl, el);
        });
      },
    },
  });
