import { h } from 'snabbdom';
import * as licon from 'lib/licon';
import * as xhr from 'lib/xhr';
import { bind } from 'lib/view';
import type LobbyController from '@/ctrl';

function initialize(ctrl: LobbyController, el: HTMLElement) {
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
    .on('submit', (e: Event) => {
      e.preventDefault();
      ctrl.filter.open = false;
      ctrl.redraw();
    });

  function changeRatingRange(e?: Event) {
    $minInput.attr('max', $maxInput.val() as string);
    $maxInput.attr('min', $minInput.val() as string);
    const txt = $minInput.val() + '-' + $maxInput.val();
    $rangeInput.val(txt);
    $ratingRange.siblings('.range').text(txt);
    if (e) save();
  }
  const rangeValues = $rangeInput.val() ? ($rangeInput.val() as string).split('-') : [];

  $minInput
    .attr({ step: '50', value: rangeValues[0] || $minInput.attr('min')! })
    .on('input', changeRatingRange);

  $maxInput
    .attr({ step: '50', value: rangeValues[1] || $maxInput.attr('max')! })
    .on('input', changeRatingRange);

  changeRatingRange();
}

export function toggle(ctrl: LobbyController, nbFiltered: number) {
  const filter = ctrl.filter;
  return h('i.toggle.toggle-filter', {
    class: { gamesFiltered: nbFiltered > 0, active: filter.open },
    hook: bind('mousedown', filter.toggle, ctrl.redraw),
    attrs: { 'data-icon': filter.open ? licon.X : licon.Gear, title: i18n.site.filterGames },
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
