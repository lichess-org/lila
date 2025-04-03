import { h, type Hooks, type VNode, type Attrs } from 'snabbdom';
import { bind } from './snabbdom';
import { toggle as baseToggle, type Toggle, myUserId } from './common';
import * as xhr from './xhr';
import * as licon from './licon';
import { storedMap } from './storage';
import { clamp } from './algo';

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
    h('button.copy-me__button.button.button-metal', { attrs: { 'data-icon': licon.Clipboard } }),
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
    });
  });
};

const heightStore = storedMap<number | undefined>(
  `lib.controls.height-store.${myUserId()}`,
  100,
  () => undefined,
);

export function verticalResizeSeparator(o: {
  key: string; // key to store the size (a generic category when id is present)
  id?: string; // optional id to store the size for a specific instance
  min: () => number;
  max: () => number;
}): VNode {
  // add these directly after the vnode they resize
  return h(
    'div.vertical-resize-separator',
    {
      hook: {
        insert: vnode => {
          const divider = vnode.elm as HTMLElement;
          const onDomChange = () => {
            const el = divider.previousElementSibling as HTMLElement;
            if (el.style.height) return;
            const height =
              (o.id ? heightStore(`${o.key}.${o.id}`) : undefined) ??
              heightStore(o.key) ?? // this should be evaluated whether id is provided or not
              el.getBoundingClientRect().height;
            el.style.flex = 'none';
            el.style.height = `${clamp(height, { min: o.min(), max: o.max() })}px`;
          };
          onDomChange();

          new MutationObserver(onDomChange).observe(divider.parentElement!, { childList: true });

          divider.addEventListener('pointerdown', down => {
            const el = divider.previousElementSibling as HTMLElement;
            const beginFrom = el.getBoundingClientRect().height - down.clientY;
            divider.setPointerCapture(down.pointerId);

            const move = (move: PointerEvent) => {
              el.style.height = `${clamp(beginFrom + move.clientY, { min: o.min(), max: o.max() })}px`;
            };

            const up = () => {
              divider.releasePointerCapture(down.pointerId);
              window.removeEventListener('pointermove', move);
              window.removeEventListener('pointerup', up);
              window.removeEventListener('pointercancel', up);
              const height = parseInt(el.style.height);
              heightStore(o.key, height);
              if (o.id) heightStore(`${o.key}.${o.id}`, height);
            };
            window.addEventListener('pointermove', move);
            window.addEventListener('pointerup', up);
            window.addEventListener('pointercancel', up);
          });
        },
      },
    },
    [h('hr', { attrs: { role: 'separator' } })],
  );
}

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
  <div class="spinner aria-label="loading">
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
