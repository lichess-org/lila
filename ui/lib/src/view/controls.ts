/* eslint no-restricted-syntax:"error" */ // no side effects allowed due to re-export by index.ts

import { h, type Hooks, type VNode, type Attrs } from 'snabbdom';
import { bind } from './snabbdom';
import { toggle as baseToggle, type Toggle } from '@/index';
import * as xhr from '@/xhr';
import * as licon from '@/licon';

export interface ToggleSettings {
  name: string;
  title?: string;
  id: string;
  checked: boolean;
  disabled?: boolean;
  cls?: string;
  change(v: boolean): void;
}

export function toggle(t: ToggleSettings, redraw: () => void): VNode {
  const fullId = 'abset-' + t.id;
  return h(
    'div.setting.' + fullId + (t.cls ? '.' + t.cls : ''),
    t.title ? { attrs: { title: t.title } } : {},
    [
      h('div.switch', [
        h('input#' + fullId + '.cmn-toggle', {
          attrs: { type: 'checkbox', checked: t.checked, disabled: !!t.disabled },
          hook: bind('change', e => t.change((e.target as HTMLInputElement).checked), redraw),
        }),
        h('label', { attrs: { for: fullId } }),
      ]),
      h('label', { attrs: { for: fullId } }, t.name),
    ],
  );
}

export function toggleBoxInit(): void {
  $('.toggle-box--toggle:not(.toggle-box--ready)').each(function (this: HTMLFieldSetElement) {
    const toggle = () => this.classList.toggle('toggle-box--toggle-off');
    $(this)
      .addClass('toggle-box--ready')
      .children('legend')
      .on('click', toggle)
      .on('keypress', e => e.key === 'Enter' && toggle());
  });
}

export function rangeConfig(read: () => number, write: (value: number) => void): Hooks {
  return {
    insert: (v: VNode) => {
      const el = v.elm as HTMLInputElement;
      el.value = '' + read();
      el.addEventListener('input', _ => write(parseInt(el.value)));
      el.addEventListener('mouseout', _ => el.blur());
    },
    update: (_, v: VNode) => {
      (v.elm as HTMLInputElement).value = `${read()}`; // force redraw on external value change
    },
  };
}

export const boolPrefXhrToggle = (prefKey: string, val: boolean, effect: () => void = site.reload): Toggle =>
  baseToggle(val, async v => {
    await xhr.text(`/pref/${prefKey}`, { method: 'post', body: xhr.form({ [prefKey]: v ? '1' : '0' }) });
    effect();
  });

export function stepwiseScroll(inner: (e: WheelEvent, scroll: boolean) => void): (e: WheelEvent) => void {
  return (e: WheelEvent) => inner(e, !e.ctrlKey); // if touchpad zooming, e.ctrlKey is true
}

export function copyMeInput(content: string, inputAttrs: Attrs = {}): VNode {
  return h('div.copy-me', [
    h('input.copy-me__target', {
      attrs: { readonly: true, spellcheck: false, value: content, ...inputAttrs },
    }),
    h('button.copy-me__button.button.button-metal', {
      attrs: { 'data-icon': licon.Clipboard, title: i18n.site.copyToClipboard },
    }),
  ]);
}

export const addPasswordVisibilityToggleListener = (): void => {
  $('.password-wrapper').each(function (this: HTMLElement) {
    const $wrapper = $(this);
    const $button = $wrapper.find('.password-reveal');
    $button.on('click', function (e: Event) {
      e.preventDefault();
      const $input = $wrapper.find('input');
      const type = $input.attr('type') === 'password' ? 'text' : 'password';
      $input.attr('type', type);
      $button.toggleClass('revealed', type === 'text');
      $input[0]?.focus();
    });
  });
};

const pathAttrs = [
  {
    'stroke-width': 3.779,
    d: 'm21.78 12.64c-1.284 8.436 8.943 12.7 14.54 17.61 3 2.632 4.412 4.442 5.684 7.93',
  },
  {
    'stroke-width': 4.157,
    d: 'm43.19 36.32c2.817-1.203 6.659-5.482 5.441-7.623-2.251-3.957-8.883-14.69-11.89-19.73-0.4217-0.7079-0.2431-1.835 0.5931-3.3 1.358-2.38 1.956-5.628 1.956-5.628',
  },
  {
    'stroke-width': 4.535,
    d: 'm37.45 2.178s-3.946 0.6463-6.237 2.234c-0.5998 0.4156-2.696 0.7984-3.896 0.6388-17.64-2.345-29.61 14.08-25.23 27.34 4.377 13.26 22.54 25.36 39.74 8.666',
  },
];

export const spinnerHtml: string = $html`
  <div class="spinner" aria-label="loading">
    <svg viewBox="-2 -2 54 54">
      <g mask="url(#mask)" fill="none">
        ${pathAttrs.map(
          (a, i) =>
            '<path id="' +
            String.fromCharCode(97 + i) +
            '" stroke-width="' +
            a['stroke-width'] +
            '" d="' +
            a.d +
            '"/>',
        )}
      </g>
    </svg>
  </div>`;

export const spinnerVdom = (box = '-2 -2 54 54'): VNode =>
  h('div.spinner', { 'aria-label': 'loading' }, [
    h('svg', { attrs: { viewBox: box } }, [
      h(
        'g',
        { attrs: { mask: 'url(#mask)', fill: 'none' } },
        pathAttrs.map(attrs => h('path', { attrs })),
      ),
    ]),
  ]);
