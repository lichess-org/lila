import { VNode, h } from 'snabbdom';
import InsightCtrl from '../ctrl';
import { InsightFilter } from '../types';
import { colorName } from 'common/colorName';
import { bind, dataIcon, onInsert } from 'common/snabbdom';
import { allOptions } from '../filter';

export function filter(ctrl: InsightCtrl): VNode {
  const noarg = ctrl.trans.noarg;
  return h('div.filter', [
    h(
      'div.filter-toggle',
      h(
        'div',
        {
          hook: bind('click', () => (ctrl.filterToggle = !ctrl.filterToggle), ctrl.redraw),
        },
        [noarg('filterGames'), h('i', { attrs: dataIcon(ctrl.filterToggle ? 'S' : 'R') })]
      )
    ),
    h(
      'div.filter-wrap',
      {
        class: {
          hide: !ctrl.filterToggle,
        },
      },
      [
        h('h2', noarg('filterGames')),
        options(ctrl, 'since', allOptions.since, (nb: number) => ctrl.trans.plural('nbDays', nb)),
        options(ctrl, 'variant', allOptions.variant, noarg),
        options(ctrl, 'color', allOptions.color, (s: 'both' | 'sente' | 'gote') =>
          s === 'both'
            ? `${colorName(noarg, 'sente', false)}/${colorName(noarg, 'gote', false)}`
            : colorName(noarg, s, false)
        ),
        options(ctrl, 'rated', allOptions.rated, (s: 'both' | 'yes' | 'no') =>
          s === 'both' ? `${noarg('yes')}/${noarg('no')}` : noarg(s)
        ),
        options(ctrl, 'computer', allOptions.computer, (s: 'both' | 'yes' | 'no') =>
          s === 'both' ? `${noarg('yes')}/${noarg('no')}` : noarg(s)
        ),
        options(ctrl, 'speeds', allOptions.speeds, ctrl.trans.noargOrCapitalize, true),
      ]
    ),
  ]);
}

function options(
  ctrl: InsightCtrl,
  key: keyof InsightFilter,
  values: string[],
  display: (value: string | number) => string,
  multiSelect: boolean = false
): VNode {
  const current = ctrl.filter[key];
  function value2option(value: string, name: string): VNode {
    return h(
      'option',
      {
        attrs: {
          value: value,
          selected: Array.isArray(current) && current.includes(value as Speed) ? 'selected' : current === value,
        },
      },
      name
    );
  }
  return h('div.options.key-' + key, [
    h('h3', key === 'computer' ? ctrl.trans('computer') : ctrl.trans(key as any)),
    h(
      'select',
      {
        attrs: { id: key, multiple: multiSelect },
        on: {
          change(e) {
            const value = (e.target as HTMLSelectElement).value;
            if (!multiSelect) ctrl.updateFilter({ [key]: value });
          },
        },
        hook: onInsert(el => {
          if (multiSelect)
            $(el).multipleSelect({
              placeholder: ctrl.trans(key as any),
              width: '100%',
              selectAll: false,
              minimumCountSelected: 10,
              onClick: function (view: { value: Speed; checked: boolean }) {
                if (view.checked && !ctrl.filter.speeds.includes(view.value)) ctrl.filter.speeds.push(view.value);
                else if (!view.checked && ctrl.filter.speeds.includes(view.value))
                  ctrl.filter.speeds = ctrl.filter.speeds.filter(s => s !== view.value);
                ctrl.updateFilter({}, true);
              },
            });
        }),
      },
      values.map(v => value2option(v, display(v)))
    ),
  ]);
}
