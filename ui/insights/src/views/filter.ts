import { bind, dataIcon } from 'common/snabbdom';
import { i18n, i18nPluralSame } from 'i18n';
import { i18nVariant } from 'i18n/variant';
import { colorName } from 'shogi/color-name';
import { type VNode, h } from 'snabbdom';
import type InsightCtrl from '../ctrl';
import { allOptions } from '../filter';
import type { InsightFilter } from '../types';
import { translateSpeed } from './util';

export function filter(ctrl: InsightCtrl): VNode {
  return h('div.filter', [
    h(
      'div.filter-toggle',
      h(
        'div',
        {
          hook: bind(
            'click',
            () => {
              ctrl.filterToggle = !ctrl.filterToggle;
            },
            ctrl.redraw,
          ),
        },
        [i18n('filterGames'), h('i', { attrs: dataIcon(ctrl.filterToggle ? 'S' : 'R') })],
      ),
    ),
    h(
      'div.filter-wrap',
      {
        class: {
          hide: !ctrl.filterToggle,
        },
      },
      [
        h('h2', i18n('filterGames')),
        options(ctrl, 'since', allOptions.since, (nb: number) => i18nPluralSame('nbDays', nb)),
        options(ctrl, 'variant', allOptions.variant, i18nVariant),
        options(ctrl, 'color', allOptions.color, (s: 'both' | 'sente' | 'gote') =>
          s === 'both'
            ? `${colorName('sente', false)}/${colorName('gote', false)}`
            : colorName(s, false),
        ),
        options(ctrl, 'rated', allOptions.rated, (s: 'both' | 'yes' | 'no') =>
          s === 'both' ? `${i18n('yes')}/${i18n('no')}` : s === 'yes' ? i18n('yes') : i18n('no'),
        ),
        options(ctrl, 'computer', allOptions.computer, (s: 'both' | 'yes' | 'no') =>
          s === 'both' ? `${i18n('yes')}/${i18n('no')}` : s === 'yes' ? i18n('yes') : i18n('no'),
        ),
        optionsSpeed(ctrl),
      ],
    ),
  ]);
}

function options(
  ctrl: InsightCtrl,
  key: keyof InsightFilter,
  values: string[],
  display: (value: string | number) => string,
): VNode {
  const current = ctrl.filter[key];
  function value2option(value: string, name: string): VNode {
    return h(
      'option',
      {
        attrs: {
          value: value,
          selected:
            Array.isArray(current) && current.includes(value as Speed)
              ? 'selected'
              : current === value,
        },
      },
      name,
    );
  }
  return h(`div.options.key-${key}`, [
    h('h3', i18ns[key]),
    h(
      'select',
      {
        attrs: { id: key },
        on: {
          change(e) {
            const value = (e.target as HTMLSelectElement).value;
            ctrl.updateFilter({ [key]: value });
          },
        },
      },
      values.map(v => value2option(v, display(v))),
    ),
  ]);
}

function optionsSpeed(ctrl: InsightCtrl): VNode {
  function speed2checkbox(speed: Speed): VNode {
    return h('label', [
      h('input', {
        attrs: {
          type: 'checkbox',
          value: speed,
          checked: ctrl.filter.speeds.includes(speed),
        },
        on: {
          change(e) {
            const isChecked = (e.target as HTMLInputElement).checked;
            if (isChecked && !ctrl.filter.speeds.includes(speed)) ctrl.filter.speeds.push(speed);
            else if (!isChecked && ctrl.filter.speeds.includes(speed))
              ctrl.filter.speeds = ctrl.filter.speeds.filter(s => s !== speed);

            ctrl.updateFilter({}, true);
          },
        },
      }),
      translateSpeed(speed),
    ]);
  }

  return h('div.options.key-speed', [
    h('h3', i18ns.speeds),
    h(
      'div',
      allOptions.speeds.map(s => speed2checkbox(s)),
    ),
  ]);
}

const i18ns: Record<keyof InsightFilter, string> = {
  since: i18n('search:from'),
  variant: i18n('variant'),
  color: i18n('insights:color'),
  rated: i18n('rated'),
  speeds: i18n('insights:speed'),
  computer: i18n('computer'),
  custom: i18n('custom'),
};
