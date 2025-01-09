import { h, VNode } from 'snabbdom';
import SetupCtrl, { type SetupDataKey } from './ctrl';
import { onInsert } from 'common/snabbdom';
import { RULES } from 'shogiops';
import { i18nVariant } from 'i18n/variant';
import { variantToId } from 'common/variant';
import { i18n } from 'i18n';

export const variantChoicesTranslated: [number, string][] = RULES.map(
  r => [variantToId(r), i18nVariant(r)] as [number, string],
);

export const timeChoices: number[] = [
  0, 0.25, 0.5, 0.75, 1, 1.5, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,
  25, 30, 35, 40, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180,
];

export const byoChoices: number[] = Array.from(Array(21), (_, i) => i).concat([
  25, 30, 35, 40, 45, 60, 90, 120, 150, 180,
]);

export const incChoices: number[] = byoChoices;

export const periodChoices: number[] = [1, 2, 3, 4, 5];

export const dayChoices: number[] = [1, 2, 3, 5, 7, 10, 14];

export const aiLevelChoices: number[] = Array.from(Array(8), (_, i) => i + 1);

export const maxRatingChoices: number[] = Array.from(Array(16), (_, i) => i * 50);
export const minRatingChoices: number[] = Array.from(Array(16), (_, i) => 750 - i * 50);

export const TimeMode = {
  Unlimited: 0, // not offered
  RealTime: 1,
  Corres: 2,
};

export const timeModeChoices: number[] = [TimeMode.RealTime, TimeMode.Corres];

export const timeModeChoicesTranslated: [number, string][] = [
  [TimeMode.RealTime, i18n('realTime')],
  [TimeMode.Corres, i18n('correspondence')],
];

export const Mode = {
  Casual: 0,
  Rated: 1,
};

export const modeChoices: number[] = [0, 1];

export const modeChoicesTranslated: [number, string][] = [
  [Mode.Casual, i18n('casual')],
  [Mode.Rated, i18n('rated')],
];

export const Position = {
  initial: 0,
  fromPosition: 1,
};

export const positionChoices: number[] = [Position.initial, Position.fromPosition];

export const positionChoicesTranslated: [number, string][] = [
  [Position.initial, i18n('default')],
  [Position.fromPosition, i18n('fromPosition')],
];

export const colorChoices = ['sente', 'random', 'gote'] as const;

export function formatMinutes(minutes: number): string {
  if (minutes === 0.25) return '¼';
  else if (minutes === 0.5) return '½';
  else if (minutes === 0.75) return '¾';
  else return `${minutes}`;
}

export function fieldId(key: SetupDataKey, value?: string | number): string {
  return `ls-${key}${value ? `-${value}` : ''}`;
}

export function select(
  ctrl: SetupCtrl,
  key: SetupDataKey,
  options: [string | number, string][],
  empty = false,
  disabledValues: (string | number)[] = [],
): VNode {
  const cur = ctrl.selected(key),
    emptyOption = empty
      ? [h('option', { attrs: { selected: cur === '', hidden: true, value: '' } }, '')]
      : [];
  return h(
    'select',
    {
      hook: onInsert(el => {
        if (key === 'variant') el.focus();
        el.addEventListener('change', e => {
          ctrl.set(key, (e.target as HTMLSelectElement).value);
          ctrl.save();
        });
      }),
      attrs: {
        id: fieldId(key),
        name: key,
      },
    },
    emptyOption.concat(
      options.map(o =>
        h(
          'option',
          {
            attrs: { value: o[0], selected: o[0] == cur },
            props: {
              disabled: disabledValues.includes(o[0]),
            },
          },
          o[1],
        ),
      ),
    ),
  );
}

export function slider(ctrl: SetupCtrl, key: SetupDataKey, options: number[], big = false): VNode {
  return h(`input.slider${big ? '.big' : ''}`, {
    attrs: {
      id: fieldId(key),
      type: 'range',
      min: 0,
      max: options.length - 1,
      step: 1,
    },
    hook: {
      insert: vnode => {
        const el = vnode.elm as HTMLInputElement,
          index = options.findIndex(n => n == ctrl.data[key]);
        el.value = `${index}`;
        el.addEventListener('input', _ => {
          const value = options[parseInt(el.value)];
          ctrl.set(key, value);
        });
        el.addEventListener('change', _ => {
          ctrl.save();
        });
        el.addEventListener('mouseout', _ => el.blur());
      },
    },
  });
}

export function radioGroup(
  ctrl: SetupCtrl,
  key: SetupDataKey,
  inputs: [number, string][],
  disabledValue?: number,
): VNode {
  return h(
    `group.radio${inputs.length === 2 ? '.dual' : ''}`,
    inputs.map(i => {
      const value = i[0],
        id = fieldId(key, value),
        name = i[1],
        disabled = disabledValue === value,
        checked = ctrl.data[key] == value;
      return h('div', { key: `${id}-${ctrl.data[key]}` }, [
        h(`input#${id}`, {
          hook: {
            insert: vnode => {
              const el = vnode.elm as HTMLInputElement;
              el.addEventListener('input', _ => {
                ctrl.set(key, parseInt(el.value));
                ctrl.save();
              });
            },
          },
          attrs: {
            type: 'radio',
            name: key,
            id,
            value,
            checked,
            disabled,
          },
        }),
        h(
          `label.required${disabled ? '.disabled' : ''}`,
          {
            attrs: {
              for: id,
            },
          },
          name,
        ),
      ]);
    }),
  );
}

export function selectNvui(
  ctrl: SetupCtrl,
  label: string,
  key: SetupDataKey,
  options: [string | number, string][] | (string | number)[],
  empty = false,
): VNode {
  const normalizedOptions: [string | number, string][] = Array.isArray(options[0])
    ? (options as [string | number, string][])
    : (options as (string | number)[]).map(o => [o, `${o}`]);

  return h('div', [
    h(
      'label',
      {
        attrs: {
          for: fieldId(key),
        },
      },
      label,
    ),
    select(ctrl, key, normalizedOptions, empty),
  ]);
}
