import { defined, type Prop } from '@/common';
import { h, type VNode } from 'snabbdom';
import { onInsert } from './snabbdom';

interface CmnToggleBase {
  id: string;
  title?: string;
  disabled?: boolean;
  redraw?: Redraw;
}

export interface CmnToggle extends CmnToggleBase {
  checked: boolean;
  propsChecked?: boolean;
  change(v: boolean): void;
}

export interface CmnToggleProp extends CmnToggleBase {
  prop: Prop<boolean>;
}

export interface CmnToggleWrap extends CmnToggle {
  name: string;
}
export interface CmnToggleWrapProp extends CmnToggleProp {
  name: string;
}

const markPointer = (input: HTMLInputElement) => {
  input.dataset.pointer = '1';
};

const markKeyboard = (input: HTMLInputElement) => {
  input.dataset.pointer = '0';
};

const blurIfPointer = (input: HTMLInputElement) => {
  // Blur only for pointer toggles, so Mousetrap hotkeys keep working.
  if (input.dataset.pointer === '1') input.blur();
  delete input.dataset.pointer;
};

export const cmnToggleProp = (opts: CmnToggleProp): VNode =>
  cmnToggle({
    ...opts,
    checked: opts.prop(),
    change: v => opts.prop(v),
  });

export const cmnToggle = (opts: CmnToggle): VNode =>
  h('span.cmn-toggle', { attrs: { role: 'button' } }, [
    h(`input#cmn-tg-${opts.id}`, {
      attrs: { type: 'checkbox', checked: opts.checked, disabled: !!opts.disabled },
      props: defined(opts.propsChecked) ? { checked: opts.propsChecked } : undefined,
      hook: onInsert((input: HTMLInputElement) => {
        input.addEventListener('keydown', () => markKeyboard(input), { passive: true });
        const innerLabel = input.parentElement?.querySelector<HTMLLabelElement>(
          `label[for="cmn-tg-${opts.id}"]`,
        );
        innerLabel?.addEventListener('pointerdown', () => markPointer(input), { passive: true });
        input.addEventListener(
          'change',
          e => {
            opts.change((e.target as HTMLInputElement).checked);
            blurIfPointer(input);
            opts.redraw?.();
          },
          { passive: true },
        );
      }),
    }),
    h('label', { attrs: { for: `cmn-tg-${opts.id}` } }),
  ]);

export const cmnToggleWrapProp = (opts: CmnToggleWrapProp): VNode =>
  cmnToggleWrap({
    ...opts,
    checked: opts.prop(),
    change: v => opts.prop(v),
  });

export const cmnToggleWrap = (opts: CmnToggleWrap): VNode =>
  h(
    'label.cmn-toggle-wrap',
    {
      ...(opts.title ? { attrs: { title: opts.title } } : {}),
      hook: onInsert((wrap: HTMLLabelElement) => {
        // Clicking the row text also toggles the input; mark it as pointer interaction.
        wrap.addEventListener(
          'pointerdown',
          () => {
            const input = wrap.querySelector<HTMLInputElement>(`#cmn-tg-${opts.id}`);
            if (input) markPointer(input);
          },
          { passive: true },
        );
      }),
    },
    [cmnToggle({ ...opts, title: undefined }), opts.name],
  );
